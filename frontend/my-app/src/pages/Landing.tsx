import { Link } from 'react-router-dom'
import { buttonClasses } from '../lib/buttonClasses'
import heroImage from '../assets/landing-hero.png'
import logo from '../assets/logo.png'

export function Landing() {
  return (
    <div className="min-h-screen bg-landing">
      <header className="flex h-[76px] items-center justify-between border-b border-border-strong bg-white px-8">
        <div className="flex items-center gap-3">
          <img alt="" className="h-9 w-9 object-contain" src={logo} />
          <span className="font-suit text-2xl text-ink">Baton 바통</span>
        </div>
        <div className="flex items-center gap-8">
          <Link className="font-suit text-lg font-semibold text-primary" to="/login">
            로그인
          </Link>
          <Link className={buttonClasses('primary', 'rounded-[10px] px-6 py-3 text-lg')} to="/signup">
            무료로 시작하기
          </Link>
        </div>
      </header>

      <div className="flex items-center px-16 py-20">
        <div className="mx-auto grid max-w-6xl grid-cols-1 items-center gap-16 md:grid-cols-2">
          <div>
            <h1 className="font-suit text-[56px] leading-[1.1] font-semibold tracking-[0.3px] text-ink">
              기다림 없이,
              <br />
              다음으로
            </h1>
            <p className="font-suit mt-6 text-xl leading-relaxed text-muted-2">
              Baton은 답장을 기다리는 시간을 없애고
              <br />
              대화를 다음 단계로 넘겨줍니다.
            </p>
            <div className="mt-10 flex gap-4">
              <Link className={buttonClasses('secondary', 'px-8 py-4 text-lg rounded-[10px]')} to="/login">
                로그인
              </Link>
              <Link className={buttonClasses('primary', 'px-8 py-4 text-lg rounded-[10px]')} to="/signup">
                무료로 시작하기
              </Link>
            </div>
          </div>
          <img alt="Baton 미리보기" className="w-full rounded-xl" src={heroImage} />
        </div>
      </div>
    </div>
  )
}
