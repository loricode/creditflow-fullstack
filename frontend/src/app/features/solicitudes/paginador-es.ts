import { MatPaginatorIntl } from '@angular/material/paginator';

export function paginadorEnEspanol(): MatPaginatorIntl {
  const intl = new MatPaginatorIntl();
  intl.itemsPerPageLabel = 'Por página';
  intl.nextPageLabel = 'Siguiente';
  intl.previousPageLabel = 'Anterior';
  intl.firstPageLabel = 'Primera';
  intl.lastPageLabel = 'Última';
  intl.getRangeLabel = (pagina, tamano, total) => {
    if (total === 0) return '0 de 0';
    const inicio = pagina * tamano + 1;
    return `${inicio} – ${Math.min(inicio + tamano - 1, total)} de ${total}`;
  };
  return intl;
}
