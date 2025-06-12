"use client";

import React, { createContext, useContext, useState, ReactNode } from "react";

interface AgentContextType {
  selectedAgent: number;
  setSelectedAgent: (agentId: number) => void;
  getAssistantId: (agentId: number) => string;
  getMcpConfigPath: (agentId: number) => string;
}

const AgentContext = createContext<AgentContextType | undefined>(undefined);

// Agent ID에 따른 Assistant ID 및 MCP Config 매핑
const AGENT_ASSISTANT_MAPPING: Record<number, string> = {
  0: "default_chat",     // 기본 대화 챗봇
  1: "agent1", // 일정 관리 Agent
  2: "agent2", // 회의자료 준비 Agent  
  3: "agent3", // 회의보고서 작성 Agent
  4: "agent4", // 보고서 전달 Agent
  5: "agent5", // 마케팅 전송 Agent
};

const AGENT_MCP_CONFIG_MAPPING: Record<number, string> = {
  0: "mcp_config0.json", // 기본 대화 챗봇 (빈 설정)
  1: "mcp_config1.json", // 일정 관리 Agent
  2: "mcp_config2.json", // 회의자료 준비 Agent  
  3: "mcp_config3.json", // 회의보고서 작성 Agent
  4: "mcp_config4.json", // 보고서 전달 Agent
  5: "mcp_config5.json", // 마케팅 전송 Agent
};

export const AgentProvider: React.FC<{ children: ReactNode }> = ({
  children,
}) => {
  const [selectedAgent, setSelectedAgent] = useState(0); // 기본값을 0으로 변경

  const getAssistantId = (agentId: number): string => {
    return AGENT_ASSISTANT_MAPPING[agentId] || "default_chat";
  };

  const getMcpConfigPath = (agentId: number): string => {
    return AGENT_MCP_CONFIG_MAPPING[agentId] || "mcp_config0.json";
  };

  return (
    <AgentContext.Provider
      value={{
        selectedAgent,
        setSelectedAgent,
        getAssistantId,
        getMcpConfigPath,
      }}
    >
      {children}
    </AgentContext.Provider>
  );
};

export const useAgentContext = (): AgentContextType => {
  const context = useContext(AgentContext);
  if (context === undefined) {
    throw new Error("useAgentContext must be used within an AgentProvider");
  }
  return context;
}; 