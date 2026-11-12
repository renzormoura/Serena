import { createContext, useContext } from 'react'

export const ContextoNotificar = createContext<(titulo: string, texto?: string) => void>(() => {})

/** Mostra uma confirmação flutuante (canto inferior), que some sozinha. Ex.: "Certificado enviado para análise". */
export function useNotificar() {
  return useContext(ContextoNotificar)
}
