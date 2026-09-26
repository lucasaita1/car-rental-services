export function buildCatalogQuery(search: string, returnDate: string): Record<string, string> {
  const query: Record<string, string> = {}
  if (search.trim()) query.busca = search.trim()
  if (returnDate) query.devolucao = returnDate
  return query
}
