import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { createBrowserRouter, Navigate, RouterProvider } from 'react-router'
import { Estrutura } from './componentes/Estrutura'
import { SomenteAutenticado } from './componentes/SomenteAutenticado'
import { SomentePapel } from './componentes/SomentePapel'
import { ProvedorDeNotificacoes } from './componentes/Notificacoes'
import { Avisos } from './paginas/Avisos'
import './index.css'
import { Cadastro } from './paginas/Cadastro'
import { Alunos } from './paginas/coordenacao/Alunos'
import { AnaliseCertificado } from './paginas/coordenacao/AnaliseCertificado'
import { FichaAluno } from './paginas/coordenacao/FichaAluno'
import { Fila } from './paginas/coordenacao/Fila'
import { Painel } from './paginas/coordenacao/Painel'
import { Equipe } from './paginas/coordenacao/Equipe'
import { CertificadoDetalhe } from './paginas/CertificadoDetalhe'
import { Certificados } from './paginas/Certificados'
import { Enviar } from './paginas/Enviar'
import { Entrar } from './paginas/Entrar'
import { Inicio } from './paginas/Inicio'
import { Regulamento } from './paginas/Regulamento'

const clienteConsultas = new QueryClient({
  defaultOptions: { queries: { retry: 1, refetchOnWindowFocus: false } },
})

const rotas = createBrowserRouter([
  { path: '/login', element: <Entrar /> },
  { path: '/cadastro', element: <Cadastro /> },
  {
    element: <SomenteAutenticado />,
    children: [
      {
        element: <Estrutura />,
        children: [
          {
            element: <SomentePapel coordenacao={false} />,
            children: [
              { index: true, element: <Inicio /> },
              { path: 'enviar', element: <Enviar /> },
              { path: 'certificados', element: <Certificados /> },
              { path: 'certificados/:id', element: <CertificadoDetalhe /> },
              { path: 'avisos', element: <Avisos /> },
            ],
          },
          {
            path: 'coordenacao',
            element: <SomentePapel coordenacao />,
            children: [
              { index: true, element: <Navigate to="painel" replace /> },
              { path: 'painel', element: <Painel /> },
              { path: 'fila', element: <Fila /> },
              { path: 'alunos', element: <Alunos /> },
              { path: 'alunos/:id', element: <FichaAluno /> },
              { path: 'certificados/:id', element: <AnaliseCertificado /> },
              { path: 'equipe', element: <Equipe /> },
            ],
          },
          { path: 'regulamento', element: <Regulamento /> },
        ],
      },
    ],
  },
  { path: '*', element: <Navigate to="/" replace /> },
])

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={clienteConsultas}>
      <ProvedorDeNotificacoes>
        <RouterProvider router={rotas} />
      </ProvedorDeNotificacoes>
    </QueryClientProvider>
  </StrictMode>,
)
