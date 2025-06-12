"""
브라우저 도구 사용을 위한 헬퍼 모듈
"""

def safe_browser_workflow(tools_runner, url, text=None, ref_selector=None):
    """
    브라우저 도구를 안전하게 사용하기 위한 워크플로우 함수
    
    Args:
        tools_runner: 도구를 실행하는 함수
        url: 방문할 URL
        text: 입력할 텍스트 (선택 사항)
        ref_selector: 요소 참조 선택자 (선택 사항)
        
    Returns:
        실행 결과
    """
    # 1. 페이지 방문
    result = tools_runner.invoke_tool(
        name="browser_navigate",
        arguments={"url": url}
    )
    
    # 2. 스냅샷 캡처
    snapshot_result = tools_runner.invoke_tool(
        name="browser_snapshot", 
        arguments={}
    )
    
    # 3. 텍스트 입력이 필요한 경우
    if text and ref_selector:
        typing_result = tools_runner.invoke_tool(
            name="browser_type",
            arguments={
                "element": "Input field",
                "ref": ref_selector,
                "text": text
            }
        )
        return typing_result
    
    return snapshot_result 