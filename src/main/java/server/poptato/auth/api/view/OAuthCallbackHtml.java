package server.poptato.auth.api.view;

public enum OAuthCallbackHtml {

    SUCCESS("""
        <html>
          <body>
            <h1>로그인이 완료되었습니다.</h1>
            <p>이 창을 닫고 데스크탑 앱으로 돌아가 주세요.</p>
          </body>
        </html>
        """),

    CANCELED("""
        <html>
          <body>
            <h1>로그인이 취소되었습니다.</h1>
            <p>이 창을 닫고 데스크탑 앱에서 다시 시도해 주세요.</p>
          </body>
        </html>
        """),

    INVALID_REQUEST("""
        <html>
          <body>
            <h1>잘못된 요청입니다.</h1>
            <p>로그인을 다시 시도해 주세요.</p>
          </body>
        </html>
        """),

    ERROR("""
        <html>
          <body>
            <h1>로그인 중 오류가 발생했습니다.</h1>
            <p>잠시 후 다시 시도해 주세요.</p>
          </body>
        </html>
        """);

    private final String html;

    OAuthCallbackHtml(String html) {
        this.html = html;
    }

    public String html() {
        return html;
    }
}