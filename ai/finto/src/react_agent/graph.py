from datetime import datetime, timezone
from typing import Dict, List, Literal, cast

from langchain_core.messages import AIMessage, SystemMessage
from langchain_core.runnables import RunnableConfig
from langgraph.graph import StateGraph
from langgraph.prebuilt import ToolNode
from pathlib import Path
from react_agent.configuration import Configuration
from react_agent.state import InputState, State
from react_agent.tools import TOOLS
from react_agent import utils
from contextlib import asynccontextmanager
from langchain_mcp_adapters.client import MultiServerMCPClient
from langgraph.prebuilt import create_react_agent
from langchain_anthropic import ChatAnthropic
from langgraph.checkpoint.memory import MemorySaver
from langchain_core.runnables import RunnableConfig
import importlib.util
import sys
import os


memory = MemorySaver()

# Agent 디렉토리 매핑
AGENT_DIRECTORIES = {
    0: "default_chat",         # 기본 대화 챗봇 (새로 추가)
    1: "schedule_management",  # 일정 관리 Agent
    2: "meeting_materials",    # 회의자료 준비 Agent
    3: "meeting_report",       # 회의보고서 작성 Agent
    4: "report_delivery",      # 보고서 전달 Agent
    5: "marketing_sender",     # 마케팅 전송 Agent
}


def load_agent_prompt(agent_id: int) -> str:
    """Agent ID에 따라 해당 agent의 프롬프트를 로드"""
    try:
        agent_dir = AGENT_DIRECTORIES.get(agent_id, "default_chat")
        prompt_file = Path(__file__).parent / "agents" / agent_dir / "prompts.py"
        
        if not prompt_file.exists():
            # 기본 프롬프트 사용
            from react_agent.configuration import DEFAULT_SYSTEM_PROMPT
            return DEFAULT_SYSTEM_PROMPT
            
        # 동적으로 프롬프트 모듈 로드
        spec = importlib.util.spec_from_file_location("agent_prompts", prompt_file)
        if spec and spec.loader:
            module = importlib.util.module_from_spec(spec)
            spec.loader.exec_module(module)
            return getattr(module, 'SYSTEM_PROMPT', '')
    except Exception as e:
        print(f"프롬프트 로드 오류: {e}")
        # 기본 프롬프트 사용
        from react_agent.configuration import DEFAULT_SYSTEM_PROMPT
        return DEFAULT_SYSTEM_PROMPT
    
    # 기본 프롬프트 반환
    from react_agent.configuration import DEFAULT_SYSTEM_PROMPT
    return DEFAULT_SYSTEM_PROMPT


async def load_agent_config(agent_id: int) -> Dict[str, Dict[str, str]]:
    """Agent ID에 따라 해당 agent의 MCP 설정을 로드"""
    try:
        agent_dir = AGENT_DIRECTORIES.get(agent_id, "default_chat")
        config_file = Path(__file__).parent / "agents" / agent_dir / "config.json"
        
        if not config_file.exists():
            return {}
            
        config = await utils.load_mcp_config_json(str(config_file))
        return config.get("mcpServers", {})
    except Exception as e:
        print(f"설정 로드 오류: {e}")
        return {}


@asynccontextmanager
async def make_graph(mcp_tools: Dict[str, Dict[str, str]]):
    if not mcp_tools:  # MCP 도구가 없는 경우 (기본 챗봇)
        model = ChatAnthropic(
            model="claude-3-5-haiku-20241022", temperature=0.0, max_tokens=8192
        )
        agent = create_react_agent(model, [], checkpointer=memory)
        yield agent
    else:
        client = MultiServerMCPClient(mcp_tools)
        tools = await client.get_tools()
        model = ChatAnthropic(
            model="claude-3-5-haiku-20241022", temperature=0.0, max_tokens=8192
        )
        agent = create_react_agent(model, tools, checkpointer=memory)
        yield agent


async def call_model(
    state: State, config: RunnableConfig
) -> Dict[str, List[AIMessage]]:
    """Call the LLM powering our "agent".

    This function prepares the prompt, initializes the model, and processes the response.

    Args:
        state (State): The current state of the conversation.
        config (RunnableConfig): Configuration for the model run.

    Returns:
        dict: A dictionary containing the model's response message.
    """
    configuration = Configuration.from_runnable_config(config)
    
    # Agent ID 추출 (기본값은 0 - 기본 대화 챗봇)
    agent_id = getattr(configuration, 'agent_id', 0)
    
    # Agent별 프롬프트 로드
    system_prompt = load_agent_prompt(agent_id)
    
    # Format the system prompt
    system_message = system_prompt.replace(
        "{{system_time}}", datetime.now(tz=timezone.utc).isoformat()
    )

    # Agent별 MCP 설정 로드
    mcp_tools = await load_agent_config(agent_id)
    print(f"Agent {agent_id} 사용 중, MCP 도구: {list(mcp_tools.keys())}")

    response = None

    async with make_graph(mcp_tools) as my_agent:
        # Create the messages list
        messages = [
            SystemMessage(content=system_message),
            *state.messages,
        ]

        # Pass messages with the correct dictionary structure
        response = cast(
            AIMessage,
            await my_agent.ainvoke(
                {"messages": messages},
                config,
            ),
        )

    # Handle the case when it's the last step and the model still wants to use a tool
    if state.is_last_step and response.tool_calls:
        return {
            "messages": [
                AIMessage(
                    id=response.id,
                    content="죄송합니다. 지정된 단계 수 내에서 질문에 대한 답변을 찾을 수 없었습니다.",
                )
            ]
        }

    # Return the model's response as a list to be added to existing messages
    return {"messages": [response["messages"][-1]]}


# Define a new graph

builder = StateGraph(State, input=InputState, config_schema=Configuration)

# Define the two nodes we will cycle between
builder.add_node(call_model)
builder.add_node("tools", ToolNode(TOOLS))

# Set the entrypoint as `call_model`
# This means that this node is the first one called
builder.add_edge("__start__", "call_model")


def route_model_output(state: State) -> Literal["__end__", "tools"]:
    """Determine the next node based on the model's output.

    This function checks if the model's last message contains tool calls.

    Args:
        state (State): The current state of the conversation.

    Returns:
        str: The name of the next node to call ("__end__" or "tools").
    """
    last_message = state.messages[-1]
    if not isinstance(last_message, AIMessage):
        raise ValueError(
            f"Expected AIMessage in output edges, but got {type(last_message).__name__}"
        )
    # If there is no tool call, then we finish
    if not last_message.tool_calls:
        return "__end__"
    # Otherwise we execute the requested actions
    return "tools"


# Add a conditional edge to determine the next step after `call_model`
builder.add_conditional_edges(
    "call_model",
    # After call_model finishes running, the next node(s) are scheduled
    # based on the output from route_model_output
    route_model_output,
)

# Add a normal edge from `tools` to `call_model`
# This creates a cycle: after using tools, we always return to the model
builder.add_edge("tools", "call_model")

# Compile the builder into an executable graph
# You can customize this by adding interrupt points for state updates
graph = builder.compile(
    interrupt_before=[],  # Add node names here to update state before they're called
    interrupt_after=[],  # Add node names here to update state after they're called
)
graph.name = "ReAct Agent"  # This customizes the name in LangSmith
