import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api } from '../api'
import { API_KEY_STORAGE_KEY } from '../api/http/config'
import logo from '../assets/logo.png'
import { buttonClasses } from '../lib/buttonClasses'

export function Signup() {
  const navigate = useNavigate()
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (localStorage.getItem(API_KEY_STORAGE_KEY)) {
      navigate('/home', { replace: true })
    }
  }, [navigate])

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!name.trim() || !email.trim() || !password.trim()) {
      setError('모든 필드를 입력해주세요.')
      return
    }
    if (password.length < 8) {
      setError('비밀번호는 8자 이상이어야 합니다.')
      return
    }
    setLoading(true)
    setError('')
    try {
      const { apiKey } = await api.signUp({ name, email, password })
      localStorage.setItem(API_KEY_STORAGE_KEY, apiKey)
      navigate('/home', { replace: true })
    } catch (err) {
      setError(err instanceof Error ? err.message : '회원가입에 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-landing">
      <header className="flex h-[76px] items-center border-b border-border-strong bg-white px-8">
        <Link className="flex items-center gap-3" to="/">
          <img alt="" className="h-9 w-9 object-contain" src={logo} />
          <span className="font-suit text-2xl text-ink">Baton 바통</span>
        </Link>
      </header>

      <div className="flex min-h-[calc(100vh-76px)] items-center justify-center py-10">
        <div className="w-[480px] rounded-2xl bg-white px-10 py-14 shadow-[0_4px_24px_rgba(0,0,0,0.08)]">
          <h1 className="font-suit text-[26px] font-semibold text-ink">회원가입</h1>
          <p className="mt-1 text-sm text-muted-2">계정을 만들어 Baton을 시작하세요</p>

          <form className="mt-7 flex flex-col gap-5" onSubmit={handleSubmit}>
            <div className="flex flex-col gap-2">
              <label className="text-[13px] font-semibold text-ink">이름</label>
              <input
                className="h-12 rounded-lg border border-border bg-[#f8f8fc] px-4 text-sm text-ink placeholder:text-muted-2 focus:border-primary focus:outline-none"
                onChange={(e) => setName(e.target.value)}
                placeholder="이름을 입력하세요"
                type="text"
                value={name}
              />
            </div>

            <div className="flex flex-col gap-2">
              <label className="text-[13px] font-semibold text-ink">이메일</label>
              <input
                className="h-12 rounded-lg border border-border bg-[#f8f8fc] px-4 text-sm text-ink placeholder:text-muted-2 focus:border-primary focus:outline-none"
                onChange={(e) => setEmail(e.target.value)}
                placeholder="example@email.com"
                type="email"
                value={email}
              />
            </div>

            <div className="flex flex-col gap-2">
              <label className="text-[13px] font-semibold text-ink">비밀번호</label>
              <input
                className="h-12 rounded-lg border border-border bg-[#f8f8fc] px-4 text-sm text-ink placeholder:text-muted-2 focus:border-primary focus:outline-none"
                onChange={(e) => setPassword(e.target.value)}
                placeholder="8자 이상 입력하세요"
                type="password"
                value={password}
              />
            </div>

            {error && <p className="text-[13px] text-red-500">{error}</p>}

            <button
              className={buttonClasses('primary', 'mt-1 w-full rounded-lg py-3 text-base')}
              disabled={loading}
              type="submit"
            >
              {loading ? '가입 중...' : '회원가입하기'}
            </button>
          </form>

          <p className="mt-6 text-center text-[13px] text-primary">
            이미 계정이 있으신가요?{' '}
            <Link className="font-semibold underline-offset-2 hover:underline" to="/login">
              로그인
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}
