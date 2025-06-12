import { useState } from "react";
import { ThumbsUp, ThumbsDown, X } from "lucide-react";
import { TooltipIconButton } from "../tooltip-icon-button";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { AnimatePresence, motion } from "framer-motion";

export type FeedbackType = "good" | "bad" | null;
export type FeedbackReason = 
  | "intent_misunderstanding" 
  | "insufficient_recommendation" 
  | "inaccurate_result" 
  | "technical_error"
  | "other";

export interface FeedbackData {
  type: FeedbackType;
  reason?: FeedbackReason;
  comment?: string;
  messageId: string;
  timestamp: number;
}

interface MessageFeedbackProps {
  messageId: string;
  disabled?: boolean;
  onFeedbackSubmit?: (feedback: FeedbackData) => void;
}

const feedbackReasons: { value: FeedbackReason; label: string }[] = [
  { value: "intent_misunderstanding", label: "의도 오인식" },
  { value: "insufficient_recommendation", label: "추천 미흡" },
  { value: "inaccurate_result", label: "결과 부정확" },
  { value: "technical_error", label: "기술적 오류" },
  { value: "other", label: "기타" },
];

export function MessageFeedback({ messageId, disabled = false, onFeedbackSubmit }: MessageFeedbackProps) {
  const [currentFeedback, setCurrentFeedback] = useState<FeedbackType>(null);
  const [showBadFeedbackDialog, setShowBadFeedbackDialog] = useState(false);
  const [selectedReason, setSelectedReason] = useState<FeedbackReason | "">("");
  const [comment, setComment] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleGoodFeedback = async () => {
    if (disabled) return;
    
    setCurrentFeedback("good");
    
    const feedbackData: FeedbackData = {
      type: "good",
      messageId,
      timestamp: Date.now(),
    };

    await submitFeedback(feedbackData);
  };

  const handleBadFeedback = () => {
    if (disabled) return;
    setShowBadFeedbackDialog(true);
  };

  const handleBadFeedbackSubmit = async () => {
    if (!selectedReason) return;
    
    setIsSubmitting(true);
    
    const feedbackData: FeedbackData = {
      type: "bad",
      reason: selectedReason as FeedbackReason,
      comment: comment.trim() || undefined,
      messageId,
      timestamp: Date.now(),
    };

    await submitFeedback(feedbackData);
    
    setCurrentFeedback("bad");
    setShowBadFeedbackDialog(false);
    setSelectedReason("");
    setComment("");
    setIsSubmitting(false);
  };

  const submitFeedback = async (feedbackData: FeedbackData) => {
    try {
      // API 호출
      const response = await fetch("/api/feedback", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(feedbackData),
      });

      if (!response.ok) {
        throw new Error("Failed to submit feedback");
      }

      // 콜백 함수 호출
      onFeedbackSubmit?.(feedbackData);
      
      console.log("Feedback submitted:", feedbackData);
    } catch (error) {
      console.error("Error submitting feedback:", error);
      // 에러 시 상태 리셋
      setCurrentFeedback(null);
    }
  };

  const resetFeedback = () => {
    setCurrentFeedback(null);
  };

  if (currentFeedback) {
    return (
      <div className="flex items-center gap-1">
        <AnimatePresence mode="wait">
          <motion.div
            key={currentFeedback}
            initial={{ opacity: 0, scale: 0.8 }}
            animate={{ opacity: 1, scale: 1 }}
            exit={{ opacity: 0, scale: 0.8 }}
            transition={{ duration: 0.15 }}
            className="flex items-center gap-1"
          >
            {currentFeedback === "good" ? (
              <ThumbsUp className="h-4 w-4 text-green-500" />
            ) : (
              <ThumbsDown className="h-4 w-4 text-red-500" />
            )}
            <span className="text-xs text-muted-foreground">
              {currentFeedback === "good" ? "도움됨" : "개선 필요"}
            </span>
          </motion.div>
        </AnimatePresence>
        <TooltipIconButton
          onClick={resetFeedback}
          variant="ghost"
          tooltip="피드백 재설정"
          className="h-6 w-6 p-1"
        >
          <X className="h-3 w-3" />
        </TooltipIconButton>
      </div>
    );
  }

  return (
    <>
      <div className="flex items-center gap-1">
        <TooltipIconButton
          onClick={handleGoodFeedback}
          variant="ghost"
          tooltip="좋은 답변"
          disabled={disabled}
          className="h-6 w-6 p-1 hover:text-green-600"
        >
          <ThumbsUp className="h-4 w-4" />
        </TooltipIconButton>
        <TooltipIconButton
          onClick={handleBadFeedback}
          variant="ghost"
          tooltip="개선이 필요한 답변"
          disabled={disabled}
          className="h-6 w-6 p-1 hover:text-red-600"
        >
          <ThumbsDown className="h-4 w-4" />
        </TooltipIconButton>
      </div>

      <Dialog open={showBadFeedbackDialog} onOpenChange={setShowBadFeedbackDialog}>
        <DialogContent className="sm:max-w-[425px]">
          <DialogHeader>
            <DialogTitle>어떤 부분이 개선되면 좋을까요? 🤔</DialogTitle>
          </DialogHeader>
          <div className="grid gap-4 py-4">
            <div className="grid gap-2">
              <label htmlFor="reason" className="text-sm font-medium">
                개선이 필요한 이유를 선택해주세요:
              </label>
              <Select value={selectedReason} onValueChange={setSelectedReason}>
                <SelectTrigger>
                  <SelectValue placeholder="사유를 선택해주세요" />
                </SelectTrigger>
                <SelectContent>
                  {feedbackReasons.map((reason) => (
                    <SelectItem key={reason.value} value={reason.value}>
                      {reason.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="grid gap-2">
              <label htmlFor="comment" className="text-sm font-medium">
                추가 의견 (선택사항):
              </label>
              <Textarea
                id="comment"
                placeholder="구체적인 개선사항이나 의견을 적어주세요..."
                value={comment}
                onChange={(e) => setComment(e.target.value)}
                className="min-h-[80px]"
              />
            </div>
          </div>
          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => setShowBadFeedbackDialog(false)}
              disabled={isSubmitting}
            >
              취소
            </Button>
            <Button
              onClick={handleBadFeedbackSubmit}
              disabled={!selectedReason || isSubmitting}
            >
              {isSubmitting ? "제출 중..." : "피드백 제출"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
} 