import { Link } from 'react-router-dom'
import { Header } from '../components/layout/Header'
import { buttonClasses } from '../lib/buttonClasses'

export function ConnectError() {
  return (
    <div className="min-h-screen bg-landing">
      <Header />
      <div className="flex flex-col items-center gap-5 px-8 py-24 text-center">
        <div className="flex size-[59px] items-center justify-center rounded-[15px] border border-amber-200 bg-amber-50">
          <span className="text-2xl">⚡</span>
        </div>
        <h1 className="font-suit text-2xl font-semibold text-ink">Slack 연결에 실패했습니다</h1>
        <p className="font-suit max-w-md text-base text-muted">
          Slack 인증 과정에서 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.
        </p>
        <div className="mt-2 flex items-center gap-4">
          <Link className="font-suit text-sm text-muted-2 hover:text-ink" to="/">
            홈으로 돌아가기
          </Link>
          <Link className={buttonClasses('primary')} to="/connect">
            다시 시도
          </Link>
        </div>
      </div>
    </div>
  )
}
