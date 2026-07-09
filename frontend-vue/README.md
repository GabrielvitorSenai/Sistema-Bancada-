# Frontend Vue - Sistema Bancada

Frontend Vue 3 desacoplado para consumir o backend Spring Boot do Sistema Bancada.

## Requisitos

- Node.js 20.15.1 ou superior
- Backend Spring Boot rodando em `http://localhost:8088`

Este projeto foi travado em Vite 5 para funcionar no PC do SENAI com Node 20.15.1.

## Instalação

```powershell
cd frontend-vue
npm install
npm run dev
```

Acesse:

```text
http://localhost:5173
```

## Teste rápido do proxy

Com o backend rodando, abra:

```text
http://localhost:5173/api/pedidos
```

Deve retornar `[]` ou a lista de pedidos do banco.

## Assets

Copie os assets do backend para o Vue:

```powershell
Copy-Item -Recurse -Force "..\Sistema Bancada\src\main\resources\static\assets" ".\public\assets"
```

As imagens da bancada devem ficar acessíveis em:

```text
/assets/bancada/Smart40-Est_pause.png
/assets/bancada/Smart40-Pro_pause.png
/assets/bancada/Smart40-Mon_pause.png
/assets/bancada/Smart40-Exp_pause.png
```

## Telas

- Dashboard
- Loja
- Pedidos
- Gestão
- Linha

## Observação

Não copie o `smart.js` antigo diretamente para o Vue. A lógica antiga deve ser migrada aos poucos para componentes, stores Pinia e chamadas API.
