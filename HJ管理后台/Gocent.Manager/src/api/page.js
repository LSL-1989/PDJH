import request from '@/utils/request'

export function getPage() {
  return request({
    url: '/api/Page/GetPage',
    method: 'post'
  })
}

export function editorPage(input) {
  return request({
    url: '/api/Page/EditorPage',
    method: 'post',
    data: input
  })
}

export function removePage(id) {
  return request({
    url: '/api/Page/RemovePage',
    method: 'post',
    params: { id }
  })
}

export function createPage(input) {
  return request({
    url: '/api/Page/CreatePage',
    method: 'post',
    data: input
  })
}
