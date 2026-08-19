import { Link } from 'react-router-dom'
import { AppShell } from '../components/layout/AppShell'
import { buttonClasses } from '../lib/buttonClasses'

export function SyncError() {
  return (
    <AppShell>
      <div className="flex flex-col items-center gap-5 py-16 text-center">
        <div className="flex size-[59px] items-center justify-center rounded-[15px] border border-amber-200 bg-amber-50">
          <span className="text-2xl">⚡</span>
        </div>
        <h1 className="font-suit text-2xl font-semibold text-ink">데이터를 불러오지 못했어요</h1>
        <p className="font-suit max-w-md text-base text-muted">
          Slack API가 응답하지 않아 최신 상태를 가져오지 못했습니다. 잠시 후 다시 시도해 주세요.
        </p>
        <div className="mt-2 flex items-center gap-4">
          <Link className="font-suit text-sm text-muted-2 hover:text-ink" to="/home">
            홈으로 돌아가기
          </Link>
          <Link className={buttonClasses('primary')} to="/connect">
            다시 시도
          </Link>
        </div>
      </div>
    </AppShell>
  )
}
