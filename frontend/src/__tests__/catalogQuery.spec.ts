import { describe, expect, it } from 'vitest'
import { buildCatalogQuery } from '@/utils/catalogQuery'

describe('buildCatalogQuery', () => {
  it('leva modelo e data de devolução para o catálogo', () => {
    expect(buildCatalogQuery('  Civic ', '2026-10-10')).toEqual({
      busca: 'Civic',
      devolucao: '2026-10-10',
    })
  })

  it('omite campos vazios', () => {
    expect(buildCatalogQuery('   ', '')).toEqual({})
  })
})
