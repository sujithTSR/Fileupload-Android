Rails.application.routes.draw do
  root to: 'pages#home'

  post 'pages/create'

  resources :pages
end
