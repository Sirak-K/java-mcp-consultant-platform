import { useState } from "react";
import { useLogin } from "~/auth/authSession";

export default function LoginPage() {
  const { login, submitting, error } = useLogin();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    login({ email, password });
  };

  return (
    <div className="panel w-full rounded p-8">
      <h1 className="text-title mb-6 text-2xl font-semibold">Logga in</h1>
      <form className="flex flex-col gap-4" onSubmit={handleSubmit}>
        <div className="flex flex-col gap-1">
          <label htmlFor="email" className="text-label text-sm font-medium">
            E-post
          </label>
          <input
            id="email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="namn@example.com"
            required
            disabled={submitting}
            className="input rounded px-3 py-2 text-sm"
          />
        </div>
        <div className="flex flex-col gap-1">
          <label htmlFor="password" className="text-label text-sm font-medium">
            Lösenord
          </label>
          <input
            id="password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="********"
            required
            disabled={submitting}
            className="input rounded px-3 py-2 text-sm"
          />
        </div>
        {error && (
          <p className="text-danger text-sm">{error}</p>
        )}
        <button
          type="submit"
          disabled={submitting || !email || !password}
          className="btn btn-main mt-2 w-full rounded px-4 py-2 text-sm font-medium"
        >
          {submitting ? "Loggar in..." : "Logga in"}
        </button>
      </form>
    </div>
  );
}
