import React, { useState } from 'react'
import { Outlet, Link, useLocation } from 'react-router-dom'
import { useAppDispatch, useAppSelector } from '../../store/hooks'
import { logout, selectCurrentUser } from '../../store/slices/authSlice'
import type { User } from '../../store/slices/authSlice'
import { useLogoutMutation } from '../../store/api/authApi'
import { logger } from '../../utils/logger'
import {
  HomeIcon,
  BuildingOfficeIcon,
  CreditCardIcon,
  DocumentTextIcon,
  Cog6ToothIcon,
  ArrowRightOnRectangleIcon,
  Bars3Icon,
  XMarkIcon,
  UserCircleIcon,
  LockClosedIcon,
} from '@heroicons/react/24/outline'
import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"
import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { cn } from "@/lib/utils"

const navigation = [
  { name: 'Dashboard', href: '/dashboard', icon: HomeIcon },
  { name: 'Organizations', href: '/organizations', icon: BuildingOfficeIcon },
  { name: 'Payments', href: '/payments', icon: CreditCardIcon },
  { name: 'Subscription', href: '/subscription', icon: DocumentTextIcon },
  { name: 'Settings', href: '/settings', icon: Cog6ToothIcon },
]

const DashboardLayout: React.FC = () => {
  const location = useLocation()
  const dispatch = useAppDispatch()
  const user = useAppSelector(selectCurrentUser)
  const [logoutMutation] = useLogoutMutation()
  const [sidebarOpen, setSidebarOpen] = useState(false)

  const handleLogout = async () => {
    try {
      await logoutMutation().unwrap()
    } catch (error) {
      logger.error('Logout failed:', error)
    } finally {
      dispatch(logout())
    }
  }

  return (
    <div className="min-h-screen relative overflow-hidden bg-gradient-to-br from-slate-50 via-white to-blue-50">
      {/* Professional subtle background pattern */}
      <div className="absolute inset-0 overflow-hidden">
        <div className="absolute inset-0" style={{
          backgroundImage: `radial-gradient(circle at 20% 20%, rgba(59, 130, 246, 0.05) 0%, transparent 40%),
                           radial-gradient(circle at 80% 80%, rgba(99, 102, 241, 0.03) 0%, transparent 40%),
                           radial-gradient(circle at 40% 60%, rgba(168, 162, 158, 0.02) 0%, transparent 40%)`
        }}></div>
      </div>

      <div className="relative z-10 flex h-screen">
        {/* Mobile sidebar */}
        <div
          className={cn(
            'fixed inset-0 flex z-50 md:hidden',
            sidebarOpen ? 'block' : 'hidden'
          )}
        >
          <div
            className="fixed inset-0 bg-gray-900/20 backdrop-blur-sm"
            onClick={() => setSidebarOpen(false)}
          />
          <div className="relative flex-1 flex flex-col max-w-xs w-full">
            <div className="absolute top-0 right-0 -mr-12 pt-2">
              <button
                type="button"
                className="ml-1 flex items-center justify-center h-10 w-10 rounded-full focus:outline-none focus:ring-2 focus:ring-inset focus:ring-gray-300 text-gray-600 hover:text-gray-800 bg-white/80 backdrop-blur-sm"
                onClick={() => setSidebarOpen(false)}
              >
                <XMarkIcon className="h-6 w-6" />
              </button>
            </div>
            <div className="backdrop-blur-xl bg-white/80 border border-gray-200/50 rounded-r-2xl h-full shadow-xl">
              <SidebarContent
                onLogout={handleLogout}
                currentPath={location.pathname}
                user={user}
                isMobile
              />
            </div>
          </div>
        </div>

        {/* Desktop sidebar */}
        <div className="hidden md:flex md:w-72 md:flex-col md:fixed md:inset-y-0 md:p-4">
          <Card variant="glass" className="flex-1 flex flex-col min-h-0">
            <SidebarContent
              onLogout={handleLogout}
              currentPath={location.pathname}
              user={user}
            />
          </Card>
        </div>

        {/* Main content */}
        <div className="flex flex-col w-0 flex-1 md:ml-72 md:pl-6">
          {/* Mobile header */}
          <div className="md:hidden">
            <div className="relative z-10 flex-shrink-0 flex h-16 backdrop-blur-xl bg-white/90 border-b border-gray-200/50 shadow-sm">
              <button
                type="button"
                className="px-4 text-gray-600 hover:text-gray-900 focus:outline-none focus:ring-2 focus:ring-inset focus:ring-blue-500"
                onClick={() => setSidebarOpen(true)}
              >
                <Bars3Icon className="h-6 w-6" />
              </button>
              <div className="flex-1 px-4 flex justify-between items-center">
                <div className="flex items-center space-x-3">
                  <LockClosedIcon className="h-6 w-6 text-gray-700" />
                  <h1 className="text-lg font-semibold text-gray-900">
                    Payment Platform
                  </h1>
                </div>
                {user && (
                  <div className="flex items-center space-x-3">
                    <span className="text-sm text-gray-700">{user.name}</span>
                    <UserCircleIcon className="h-8 w-8 text-gray-500" />
                  </div>
                )}
              </div>
            </div>
          </div>

          {/* Page content */}
          <main className="flex-1 relative overflow-y-auto focus:outline-none p-4 md:p-6">
            <div className="max-w-7xl mx-auto">
              <Card variant="glass" className="p-6 md:p-8 min-h-[calc(100vh-8rem)] md:min-h-[calc(100vh-6rem)]">
                <Outlet />
              </Card>
            </div>
          </main>
        </div>
      </div>
    </div>
  )
}

type SidebarContentProps = {
  onLogout: () => void
  currentPath: string
  user: User | null
  isMobile?: boolean
}

const SidebarContent: React.FC<SidebarContentProps> = ({
  onLogout,
  currentPath,
  user,
  isMobile = false,
}) => {
  return (
    <>
      {/* Logo */}
      <div className="flex items-center flex-shrink-0 px-6 py-6 border-b border-gray-200/30">
        <div className="flex items-center">
          <div className="w-9 h-9 bg-gradient-to-br from-blue-600 to-indigo-600 rounded-lg flex items-center justify-center shadow-sm">
            <LockClosedIcon className="h-5 w-5 text-white" />
          </div>
          <span className="ml-3 text-lg font-semibold text-gray-900">
            Payment Platform
          </span>
        </div>
      </div>

      {/* Navigation */}
      <div className="flex-1 flex flex-col overflow-y-auto px-4 py-4">
        <nav className="flex-1 space-y-1">
          {navigation.map(item => {
            const isActive =
              currentPath === item.href ||
              (item.href !== '/dashboard' && currentPath.startsWith(item.href))

            return (
              <Link
                key={item.name}
                to={item.href}
                className={cn(
                  'group flex items-center px-3 py-2.5 text-sm font-medium rounded-lg transition-all duration-200',
                  isActive
                    ? 'bg-blue-50/80 border-r-2 border-blue-600 text-blue-700 shadow-sm backdrop-blur-sm'
                    : 'text-gray-600 hover:bg-white/40 hover:text-gray-900 hover:backdrop-blur-sm'
                )}
              >
                <item.icon
                  className={cn(
                    'mr-3 flex-shrink-0 h-5 w-5',
                    isActive
                      ? 'text-blue-600'
                      : 'text-gray-400 group-hover:text-gray-600'
                  )}
                />
                {item.name}
              </Link>
            )
          })}
        </nav>
      </div>

      {/* User section */}
      <div className="flex-shrink-0 border-t border-gray-200/30 p-4">
        <div className="flex items-center w-full p-3 rounded-lg bg-white/20 hover:bg-white/30 transition-colors duration-200 backdrop-blur-sm">
          <Avatar className="h-8 w-8">
            <AvatarFallback className="bg-gradient-to-br from-gray-400 to-gray-500 text-white text-xs">
              {user?.name?.charAt(0) || 'U'}
            </AvatarFallback>
          </Avatar>
          <div className="ml-3 flex-1 min-w-0">
            <p className="text-sm font-medium text-gray-900 truncate">
              {user?.name || 'Demo User'}
            </p>
            <p className="text-xs text-gray-600 truncate">
              {user?.email || 'demo@example.com'}
            </p>
          </div>
          <Button
            variant="ghost"
            size="icon"
            onClick={onLogout}
            className="flex-shrink-0 h-8 w-8 text-gray-500 hover:text-gray-700 hover:bg-white/40"
            title="Sign out"
          >
            <ArrowRightOnRectangleIcon className="h-4 w-4" />
          </Button>
        </div>
      </div>
    </>
  )
}

export default DashboardLayout
