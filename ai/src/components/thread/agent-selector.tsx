"use client";

import { useState, useRef, useEffect } from "react";
import { Button } from "../ui/button";
import { Badge } from "../ui/badge";
import { 
  Calendar, 
  FileText, 
  Edit3, 
  Send, 
  Mail,
  ChevronDown,
  ChevronUp,
  MessageCircle
} from "lucide-react";
import { cn } from "@/lib/utils";

export interface AgentType {
  id: number;
  name: string;
  description: string;
  icon: React.ReactNode;
  color: string;
  tools: string[];
}

const AGENTS: AgentType[] = [
  {
    id: 0,
    name: "기본 대화 챗봇",
    description: "일반적인 대화 및 질문 답변",
    icon: <MessageCircle className="w-4 h-4" />,
    color: "bg-gray-100 text-gray-800 border-gray-200",
    tools: ["기본 대화"]
  },
  {
    id: 1,
    name: "일정 관리 Agent",
    description: "일정 등록",
    icon: <Calendar className="w-4 h-4" />,
    color: "bg-blue-100 text-blue-800 border-blue-200",
    tools: ["Google Calendar", "Playwright"]
  },
  {
    id: 2,
    name: "회의자료 준비 Agent",
    description: "법률 문서 검색 및 회의자료 준비",
    icon: <FileText className="w-4 h-4" />,
    color: "bg-green-100 text-green-800 border-green-200",
    tools: ["Legal Qdrant"]
  },
  {
    id: 3,
    name: "회의보고서 작성 Agent",
    description: "법률 문서 기반 회의보고서 작성",
    icon: <Edit3 className="w-4 h-4" />,
    color: "bg-purple-100 text-purple-800 border-purple-200",
    tools: ["Legal Qdrant"]
  },
  {
    id: 4,
    name: "보고서 전달 Agent",
    description: "회의보고서를 업로드 및 전달",
    icon: <Send className="w-4 h-4" />,
    color: "bg-orange-100 text-orange-800 border-orange-200",
    tools: ["Notion API"]
  },
  {
    id: 5,
    name: "전송 Agent",
    description: "메일/슬랙 전송",
    icon: <Mail className="w-4 h-4" />,
    color: "bg-red-100 text-red-800 border-red-200",
    tools: ["Slack", "Gmail"]
  }
];

interface AgentSelectorProps {
  selectedAgent: number;
  onAgentChange: (agentId: number) => void;
  compact?: boolean;
}

export function AgentSelector({ selectedAgent, onAgentChange, compact = false }: AgentSelectorProps) {
  const [isExpanded, setIsExpanded] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const currentAgent = AGENTS.find(agent => agent.id === selectedAgent) || AGENTS[0];

  // 외부 클릭 시 토글 닫기
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsExpanded(false);
      }
    };

    if (isExpanded) {
      document.addEventListener('mousedown', handleClickOutside);
    }

    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [isExpanded]);

  if (compact) {
    return (
      <div className="relative" ref={dropdownRef}>
        <Button
          variant="outline"
          onClick={() => setIsExpanded(!isExpanded)}
          className={cn(
            "flex items-center gap-2 px-3 py-1.5 h-auto shadow-sm border transition-all duration-200",
            currentAgent.color,
            isExpanded && "rounded-b-none"
          )}
        >
          {currentAgent.icon}
          <span className="text-sm font-medium">{currentAgent.name}</span>
          {isExpanded ? <ChevronUp className="w-3 h-3" /> : <ChevronDown className="w-3 h-3" />}
        </Button>

        {isExpanded && (
          <div className="absolute top-full left-0 right-0 z-50 bg-white border border-t-0 border-gray-200 rounded-b-lg shadow-lg min-w-[280px]">
            {AGENTS.map((agent) => (
              <button
                key={agent.id}
                onClick={() => {
                  onAgentChange(agent.id);
                  setIsExpanded(false);
                }}
                className={cn(
                  "w-full flex items-center gap-2 px-3 py-2 text-left hover:bg-gray-50 transition-colors",
                  selectedAgent === agent.id && "bg-gray-100",
                  agent.id === AGENTS.length - 1 && "rounded-b-lg"
                )}
              >
                <div className={cn("p-1.5 rounded-full", agent.color)}>
                  {agent.icon}
                </div>
                <div className="flex-1">
                  <div className="font-medium text-sm text-gray-900">{agent.name}</div>
                  <div className="text-xs text-gray-600">{agent.description}</div>
                </div>
              </button>
            ))}
          </div>
        )}
      </div>
    );
  }

  return (
    <div className="relative w-full" ref={dropdownRef}>
      <Button
        variant="outline"
        onClick={() => setIsExpanded(!isExpanded)}
        className={cn(
          "flex items-center gap-3 px-4 py-2 h-auto min-h-[60px] shadow-sm border-2 transition-all duration-200 w-full",
          currentAgent.color,
          isExpanded && "rounded-b-none"
        )}
      >
        <div className="flex items-center gap-3 flex-1">
          {currentAgent.icon}
          <div className="text-left">
            <div className="font-semibold text-sm">{currentAgent.name}</div>
            <div className="text-xs opacity-80">{currentAgent.description}</div>
          </div>
        </div>
        {isExpanded ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
      </Button>

      {isExpanded && (
        <div className="absolute top-full left-0 right-0 z-50 bg-white border-2 border-t-0 border-gray-200 rounded-b-lg shadow-lg">
          {AGENTS.map((agent) => (
            <button
              key={agent.id}
              onClick={() => {
                onAgentChange(agent.id);
                setIsExpanded(false);
              }}
              className={cn(
                "w-full flex items-center gap-3 px-4 py-3 text-left hover:bg-gray-50 transition-colors",
                selectedAgent === agent.id && "bg-gray-100",
                agent.id === AGENTS.length - 1 && "rounded-b-lg"
              )}
            >
              <div className={cn("p-2 rounded-full", agent.color)}>
                {agent.icon}
              </div>
              <div className="flex-1">
                <div className="font-semibold text-sm text-gray-900">{agent.name}</div>
                <div className="text-xs text-gray-600 mb-1">{agent.description}</div>
                <div className="flex gap-1 flex-wrap">
                  {agent.tools.map((tool) => (
                    <Badge key={tool} variant="secondary" className="text-xs py-0 px-2">
                      {tool}
                    </Badge>
                  ))}
                </div>
              </div>
            </button>
          ))}
        </div>
      )}
    </div>
  );
} 