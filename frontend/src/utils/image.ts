export const ACCEPTED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp']
export const MAX_IMAGE_BYTES = 5 * 1024 * 1024

export function validateImage(file: File): string {
  if (!ACCEPTED_IMAGE_TYPES.includes(file.type))
    return 'Formato não suportado. Use JPG, PNG ou WebP.'
  if (file.size > MAX_IMAGE_BYTES) return 'A imagem deve ter no máximo 5 MB.'
  return ''
}

export function validatePdf(file: File): string {
  if (file.type !== 'application/pdf' && !file.name.toLowerCase().endsWith('.pdf'))
    return 'Envie a CNH em PDF.'
  if (file.size > MAX_IMAGE_BYTES) return 'O PDF deve ter no máximo 5 MB.'
  return ''
}
