import React, { useState, useEffect } from 'react'
import { Outlet, Link, useLocation, useNavigate } from 'react-router-dom'
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
  MagnifyingGlassIcon,
} from '@heroicons/react/24/outline'

import { useAppDispatch, useAppSelector } from '../../store/hooks'
import { logout, selectCurrentUser } from '../../store/slices/authSlice'
import { useLogoutMutation } from '../../store/api/authApi'
import { logger } from '../../utils/logger'
import {
  useFocusTrap,
  useKeyboardNavigation,
  useAccessibilityContext,
} from '../../hooks/useAccessibility'
import { CommandPalette } from '../ui/InteractionPatterns'

import { Button } from '@/components/ui/button'
import { Avatar, AvatarFallback } from '@/components/ui/Avatar'
import { cn } from '@/lib/utils'

const navigation = [
  {
    name: 'Dashboard',
    href: '/dashboard',
    icon: HomeIcon,
    description: 'View your dashboard overview and statistics'
  },
  {
    name: 'Organizations',
    href: '/organizations',
    icon: BuildingOfficeIcon,
    description: 'Manage your organizations and memberships'
  },
  {
    name: 'Payments',
    href: '/payments',
    icon: CreditCardIcon,
    description: 'View payment history and manage payment methods'
  },
  {
    name: 'Subscription',
    href: '/subscription',
    icon: DocumentTextIcon,
    description: 'Manage your subscription and billing'
  },
  {
    name: 'Settings',
    href: '/settings',
    icon: Cog6ToothIcon,
    description: 'Configure your account settings and preferences'
  },
]

const AccessibleDashboardLayout: React.FC = () => {
  const location = useLocation()
  const navigate = useNavigate()
  const dispatch = useAppDispatch()
  const user = useAppSelector(selectCurrentUser)
  const [logoutMutation] = useLogoutMutation()
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const [commandPaletteOpen, setCommandPaletteOpen] = useState(false)

  // Accessibility hooks
  const { announce, components: { AnnouncementRegion, StatusRegion, SkipLink } } = useAccessibilityContext()
  const sidebarFocusTrap = useFocusTrap(sidebarOpen)
  const { containerRef: navRef } = useKeyboardNavigation({
    direction: 'vertical',
    onSelect: (index) => {
      const link = navigation[index]
      if (link) {
        navigate(link.href)
        announce(`Navigating to ${link.name}`)
      }
    }
  })

  const handleLogout = async () => {
    announce('Logging out...')
    try {
      await logoutMutation().unwrap()
      announce('Successfully logged out')
    } catch (error) {
      logger.error('Logout failed:', error)
      announce('Logout failed. Please try again.')
    } finally {
      dispatch(logout())
    }
  }

  const openSidebar = () => {
    setSidebarOpen(true)
    announce('Navigation menu opened')
  }

  const closeSidebar = () => {
    setSidebarOpen(false)
    announce('Navigation menu closed')
  }

  // Keyboard shortcuts
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      // Command palette shortcut (Cmd+K or Ctrl+K)
      if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
        e.preventDefault()
        setCommandPaletteOpen(true)
        announce('Command palette opened')
      }

      // Close sidebar with Escape
      if (e.key === 'Escape' && sidebarOpen) {
        closeSidebar()
      }
    }

    document.addEventListener('keydown', handleKeyDown)
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [sidebarOpen])

  // Command palette items
  const commandItems = [
    ...navigation.map(item => ({
      id: item.href,
      title: `Go to ${item.name}`,
      subtitle: item.description,
      action: () => navigate(item.href),
      icon: item.icon,
      group: 'Navigation'
    })),
    {
      id: 'logout',
      title: 'Sign Out',
      subtitle: 'Log out of your account',
      action: handleLogout,
      icon: ArrowRightOnRectangleIcon,
      group: 'Account'
    }
  ]

  // Get current page title for accessibility
  const getCurrentPageTitle = () => {
    const current = navigation.find(item =>
      location.pathname === item.href ||
      (item.href !== '/dashboard' && location.pathname.startsWith(item.href + '/')) ||
      (item.href === '/dashboard' && location.pathname === '/')
    )
    return current ? current.name : 'Payment Platform'
  }

  // Announce page changes
  useEffect(() => {
    const pageTitle = getCurrentPageTitle()
    announce(`${pageTitle} page loaded`)
  }, [location.pathname])

  return (
    <>
      {/* Skip Links */}
      <SkipLink href="#main-content">Skip to main content</SkipLink>
      <SkipLink href="#navigation">Skip to navigation</SkipLink>

      {/* Live Regions */}
      <AnnouncementRegion />
      <StatusRegion />

      <div className="h-screen flex bg-gray-50">
        <div className="flex w-full">
          {/* Mobile sidebar */}
          <div
            className={cn(
              'fixed inset-0 flex z-50 md:hidden transition-opacity duration-300',
              sidebarOpen ? 'opacity-100' : 'opacity-0 pointer-events-none'
            )}
            role="dialog"
            aria-modal="true"
            aria-labelledby="sidebar-title"
          >
            <div
              className="fixed inset-0 bg-gray-900/20 backdrop-blur-sm"
              onClick={closeSidebar}
              aria-label="Close navigation menu"
            />
            <div
              ref={sidebarFocusTrap as React.RefObject<HTMLDivElement>}
              className={cn(
                'relative flex-1 flex flex-col max-w-xs w-full transform transition-transform duration-300 ease-in-out',
                sidebarOpen ? 'translate-x-0' : '-translate-x-full'
              )}
            >
              <div className="absolute top-0 right-0 -mr-12 pt-2">
                <button
                  type="button"
                  className="ml-1 flex items-center justify-center h-10 w-10 rounded-full focus:outline-none focus:ring-2 focus:ring-inset focus:ring-gray-300 text-gray-600 hover:text-gray-800 bg-white/80 backdrop-blur-sm"
                  onClick={closeSidebar}
                  aria-label="Close navigation menu"
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
                  navRef={navRef as React.RefObject<HTMLElement>}
                  onNavigate={(path) => {
                    navigate(path)
                    closeSidebar()
                  }}
                />
              </div>
            </div>
          </div>

          {/* Desktop sidebar */}
          <div className="hidden md:flex md:w-64 md:flex-col">
            <div className="bg-white border-r border-gray-200 flex-1 flex flex-col min-h-0">
              <SidebarContent
                onLogout={handleLogout}
                currentPath={location.pathname}
                user={user}
                navRef={navRef as React.RefObject<HTMLElement>}
                onNavigate={navigate}
              />
            </div>
          </div>

          {/* Main content */}
          <div className="flex flex-col flex-1 overflow-hidden">
            {/* Mobile header */}
            <div className="md:hidden">
              <div className="relative z-10 flex-shrink-0 flex h-16 backdrop-blur-xl bg-white/90 border-b border-gray-200/50 shadow-sm">
                <button
                  type="button"
                  className="px-4 text-gray-600 hover:text-gray-900 focus:outline-none focus:ring-2 focus:ring-inset focus:ring-blue-500"
                  onClick={openSidebar}
                  aria-label="Open navigation menu"
                  aria-expanded={sidebarOpen}
                  aria-controls="mobile-sidebar"
                >
                  <Bars3Icon className="h-6 w-6" />
                </button>
                <div className="flex-1 px-4 flex justify-between items-center">
                  <div className="flex items-center space-x-3">
                    <LockClosedIcon className="h-6 w-6 text-gray-700" aria-hidden="true" />
                    <h1 className="text-lg font-semibold text-gray-900">
                      Payment Platform
                    </h1>
                  </div>
                  {user && (
                    <div className="flex items-center space-x-3">
                      <span className="text-sm text-gray-700">{user.firstName ?? user.email}</span>
                      <UserCircleIcon className="h-8 w-8 text-gray-500" aria-hidden="true" />
                    </div>
                  )}

                  {/* Command palette trigger */}
                  <button
                    onClick={() => setCommandPaletteOpen(true)}
                    className="ml-3 p-2 text-gray-400 hover:text-gray-600 focus:outline-none focus:ring-2 focus:ring-blue-500 rounded-md"
                    aria-label="Open command palette"
                    title="Open command palette (⌘K)"
                  >
                    <MagnifyingGlassIcon className="h-5 w-5" />
                  </button>
                </div>
              </div>
            </div>

            {/* Page content */}
            <main
              id="main-content"
              className="flex-1 overflow-y-auto p-4 focus:outline-none"
              tabIndex={-1}
              role="main"
              aria-label={`${getCurrentPageTitle()} main content`}
            >
              <Outlet />
            </main>
          </div>
        </div>

        {/* Command Palette */}
        <CommandPalette
          isOpen={commandPaletteOpen}
          onClose={() => setCommandPaletteOpen(false)}
          items={commandItems}
          placeholder="Search for actions, pages, or settings..."
        />
      </div>
    </>
  )
}

interface SidebarContentProps {
  onLogout: () => void
  currentPath: string
  user: any
  isMobile?: boolean
  navRef?: React.RefObject<HTMLElement>
  onNavigate: (path: string) => void
}

const SidebarContent: React.FC<SidebarContentProps> = ({
  onLogout,
  currentPath,
  user,
  isMobile: _isMobile = false, // Reserved for future mobile optimizations
  navRef,
  onNavigate
}) => {
  // Suppress unused variable warning - reserved for future use
  void _isMobile;
  return (
    <>
      {/* Logo */}
      <div className="flex items-center flex-shrink-0 px-4 py-4 border-b border-gray-200">
        <div className="flex items-center">
          <div className="w-8 h-8 gradient-brand rounded-lg flex items-center justify-center">
            <LockClosedIcon className="h-4 w-4 text-white" aria-hidden="true" />
          </div>
          <span className="ml-2 text-base font-semibold text-gray-900">
            Payment Platform
          </span>
        </div>
      </div>

      {/* Navigation */}
      <nav
        id="navigation"
        className="flex-1 flex flex-col overflow-y-auto px-3 py-3"
        aria-label="Main navigation"
        ref={navRef}
      >
        <ul className="flex-1 space-y-1" role="list">
          {navigation.map((item, index) => {
            const isActive = currentPath === item.href ||
              (item.href !== '/dashboard' && currentPath.startsWith(item.href + '/')) ||
              (item.href === '/dashboard' && currentPath === '/')

            return (
              <li key={item.name} role="none">
                <Link
                  to={item.href}
                  data-keyboard-nav
                  onClick={() => onNavigate(item.href)}
                  className={cn(
                    'group flex items-center px-3 py-2 text-sm font-medium rounded-lg transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-inset',
                    isActive
                      ? 'bg-primary-50 text-primary-700 border-r-2 border-primary-600'
                      : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
                  )}
                  aria-current={isActive ? 'page' : undefined}
                  aria-describedby={`nav-desc-${index}`}
                >
                  <item.icon
                    className={cn(
                      'mr-3 flex-shrink-0 h-4 w-4',
                      isActive
                        ? 'text-primary-600'
                        : 'text-gray-400 group-hover:text-gray-600'
                    )}
                    aria-hidden="true"
                  />
                  {item.name}
                  <span id={`nav-desc-${index}`} className="sr-only">
                    {item.description}
                  </span>
                </Link>
              </li>
            )
          })}
        </ul>
      </nav>

      {/* User section */}
      <div className="flex-shrink-0 border-t border-gray-200 p-3">
        <div className="flex items-center w-full p-2 rounded-lg hover:bg-gray-50 transition-colors">
          <Avatar className="h-7 w-7">
            <AvatarFallback className="bg-gray-500 text-white text-xs">
              {user?.name?.charAt(0) ?? 'U'}
            </AvatarFallback>
          </Avatar>
          <div className="ml-2 flex-1 min-w-0">
            <p className="text-xs font-medium text-gray-900 truncate">
              {user?.name ?? 'Demo User'}
            </p>
            <p className="text-xs text-gray-600 truncate">
              {user?.email ?? 'demo@example.com'}
            </p>
          </div>
          <Button
            variant="ghost"
            size="sm"
            onClick={onLogout}
            className="flex-shrink-0 h-7 w-7 p-0 text-gray-500 hover:text-gray-700 focus:ring-2 focus:ring-blue-500"
            aria-label="Sign out"
          >
            <ArrowRightOnRectangleIcon className="h-3 w-3" />
          </Button>
        </div>
      </div>
    </>
  )
}

export default AccessibleDashboardLayout