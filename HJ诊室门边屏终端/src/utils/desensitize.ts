/** 中文姓名脱敏（与 packages/common/src/desensitize.js 保持一致） */
export function desensitizeName(val: string | null | undefined): string {
  if (!val || val === '') return ''
  const name = val.trim()
  if (name.length === 2) {
    return `${name.substring(0, 1)} * `
  }
  if (name.length === 3) {
    return `${name.substring(0, 1)} * ${name.substring(2, 3)}`
  }
  if (name.length === 4) {
    return `${name.substring(0, 1)} *  * ${name.substring(3, 4)}`
  }
  if (name.length > 4) {
    let result = name.substring(0, 1)
    for (let i = 0; i < name.length - 1; i++) {
      result += ' * '
    }
    return result
  }
  return name
}
