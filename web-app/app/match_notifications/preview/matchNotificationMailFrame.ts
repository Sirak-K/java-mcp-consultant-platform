export function matchNotificationMailFrameDocument(htmlBody: string): string {
  if (
    htmlBody.trim().toLowerCase().startsWith("<!doctype") ||
    htmlBody.trim().toLowerCase().startsWith("<html")
  ) {
    return htmlBody;
  }
  return `<!doctype html>
<html>
<head>
  <meta charset="utf-8" />
  <base target="_blank" />
  <style>
    @import url("https://fonts.googleapis.com/css2?family=Orbitron:wght@500;600;700&family=Rajdhani:wght@500;600;700&display=swap");

    html {
      background: #020617;
    }
    body {
      margin: 0;
      min-height: 100vh;
      padding: 30px;
      background:
        radial-gradient(circle at 12% 0%, rgba(34, 211, 238, 0.16), transparent 34%),
        linear-gradient(180deg, #050b18 0%, #020617 100%);
      color: #dceaf8;
      font-family: "Rajdhani", "Bahnschrift", "Agency FB", "BankGothic Md BT", "Eurostile", sans-serif;
      font-size: 16px;
      line-height: 1.6;
      font-stretch: semi-condensed;
    }
    p {
      margin: 0 0 1rem;
    }
    h2 {
      color: #f7fbff;
      letter-spacing: 0;
      font-family: "Orbitron", "Bahnschrift", "Agency FB", "BankGothic Md BT", "Eurostile", sans-serif;
      font-size: 2.15rem;
      line-height: 1.12;
      margin: 2rem 0 1.25rem;
      text-shadow: 0 0 18px rgba(34, 211, 238, 0.22);
    }
    h3 {
      color: #a5f3fc;
      letter-spacing: 0;
      font-family: "Orbitron", "Bahnschrift", "Agency FB", "BankGothic Md BT", "Eurostile", sans-serif;
      font-size: 1.2rem;
      line-height: 1.25;
      margin: 1.9rem 0 0.85rem;
    }
    strong {
      color: #ffffff;
      font-weight: 800;
    }
    body > p:first-child {
      max-width: 1040px;
      color: #e2f6ff;
      font-size: 1.04rem;
      font-weight: 650;
    }
    img { max-width: 100%; height: auto; }
    a { color: #22d3ee; }
  </style>
</head>
<body>${htmlBody}</body>
</html>`;
}
