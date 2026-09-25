import { describe, expect, it } from 'vitest'
import { MAX_IMAGE_BYTES, validateImage } from '@/utils/image'

function fakeFile(type: string, size: number): File {
  const file = new File(['x'], 'foto', { type })
  Object.defineProperty(file, 'size', { value: size })
  return file
}

describe('validateImage', () => {
  it('aceita JPG, PNG e WebP até 5 MB', () => {
    expect(validateImage(fakeFile('image/jpeg', 1000))).toBe('')
    expect(validateImage(fakeFile('image/png', MAX_IMAGE_BYTES))).toBe('')
    expect(validateImage(fakeFile('image/webp', 10))).toBe('')
  })

  it('recusa outros formatos', () => {
    expect(validateImage(fakeFile('application/pdf', 10))).toContain('Formato')
    expect(validateImage(fakeFile('image/gif', 10))).toContain('Formato')
  })

  it('recusa arquivos acima de 5 MB', () => {
    expect(validateImage(fakeFile('image/jpeg', MAX_IMAGE_BYTES + 1))).toContain('5 MB')
  })
})
