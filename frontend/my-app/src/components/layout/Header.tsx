import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../../api'
import logo from '../../assets/logo.png'
import userCircle from '../../assets/user-circle.svg'

export function Header() {
  const [userName, setUserName] = useState('')

  useEffect(() => {
    api.getCurrentUser().then((user) => setUserName(user.name))
  }, [])

  return (
    <header className="flex h-[76px] items-center justify-between border-b border-border-strong bg-white px-8">
      <Link className="flex items-center gap-3" to="/home">
        <img alt="" className="h-9 w-9 object-contain" src={logo} />
        <span className="font-suit text-2xl text-ink">Baton 바통</span>
      </Link>
      <div className="flex items-center gap-3">
        <span className="font-suit text-lg font-semibold text-ink">{userName}</span>
        <img alt="" className="h-10 w-10" src={userCircle} />
      </div>
    </header>
  )
}
