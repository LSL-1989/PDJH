import defaultSettings from '@/settings'

export default function getPageTitle(pageTitle) {
  const title = (window.$sysConfig && window.$sysConfig['SystemTitle']) ? window.$sysConfig['SystemTitle'] : (defaultSettings.title || 'Vue Admin Template')

  if (pageTitle) {
    return `${pageTitle} - ${title}`
  }
  return `${title}`
}
