import { test } from './fixtures'
import {
  assertNoErrorBoundary,
  navigateToSection,
  performDemoLogin,
  verifyNavigationLinks,
} from './utils/test-utils'

const FLOW_LINKS = [
  { name: 'Organizations', href: '/organizations', testId: 'nav-organizations' },
  { name: 'Payments', href: '/payments', testId: 'nav-payments' },
  { name: 'Subscription', href: '/subscription', testId: 'nav-subscription' },
  { name: 'Settings', href: '/settings', testId: 'nav-settings' },
]

test.describe('Application Flow Test', () => {
  test('should test complete app flow and identify issues', async ({ page, evidenceCollector }) => {
    console.log('Starting app flow test...')

    try {
      await performDemoLogin(page)
      await assertNoErrorBoundary(page)

      await verifyNavigationLinks(page, [{ name: 'Dashboard', href: '/dashboard', testId: 'nav-dashboard' }, ...FLOW_LINKS])
      await evidenceCollector.takeStepScreenshot('dashboard', { fullPage: true })

      for (const link of FLOW_LINKS) {
        console.log(`Testing navigation to ${link.name}...`)
        await navigateToSection(page, link)
        await evidenceCollector.takeStepScreenshot(`after-${link.name.toLowerCase()}`, { fullPage: true })
      }

      console.log('🎉 Application flow test completed successfully!')
    } catch (error) {
      console.log('❌ Application flow failed:', (error as Error).message)
      await evidenceCollector.takeStepScreenshot('application-flow-failure', { fullPage: true })
      throw error
    }
  })
})
