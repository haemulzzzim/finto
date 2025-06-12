"""Define the configurable parameters for the agent."""

from __future__ import annotations

from dataclasses import dataclass, field, fields
from typing import Annotated, Optional

from langchain_core.runnables import RunnableConfig, ensure_config

# 기본 시스템 프롬프트 (agent별 동적 로딩이 실패할 경우 사용)
DEFAULT_SYSTEM_PROMPT = """당신은 도움이 되는 AI 어시스턴트입니다. 이름은 '금융 MCP 에이전트'입니다.
사용자의 질문에 정확하고 유용한 답변을 제공합니다.
한국어로 대답해주세요.

시스템 시간: {system_time}"""


@dataclass(kw_only=True)
class Configuration:
    """The configuration for the agent."""

    system_prompt: str = field(
        default=DEFAULT_SYSTEM_PROMPT,
        metadata={
            "description": "The system prompt to use for the agent's interactions. "
            "This prompt sets the context and behavior for the agent."
        },
    )

    mcp_tools: str = field(
        default="mcp_config.json",
        metadata={"description": "The path to the MCP tools configuration file."},
    )

    agent_id: int = field(
        default=0,
        metadata={
            "description": "The ID of the agent to use (0: default_chat, 1: schedule_management, etc.)"
        },
    )

    recursion_limit: int = field(
        default=30,
        metadata={
            "description": "The maximum number of recursive calls that Agent can make."
        },
    )

    @classmethod
    def from_runnable_config(
        cls, config: Optional[RunnableConfig] = None
    ) -> Configuration:
        """Create a Configuration instance from a RunnableConfig object."""
        config = ensure_config(config)
        configurable = config.get("configurable") or {}
        _fields = {f.name for f in fields(cls) if f.init}
        return cls(**{k: v for k, v in configurable.items() if k in _fields})
