## api-gateway: тестовые учётные данные

Пароли в конфигурации хранятся в виде BCrypt-хешей (`config-repo/infra/api-gateway.yml`,
блок `app.security.users`). Для ручной проверки через Web UI, curl или Postman:

| Логин | Пароль | Роли |
|-------|--------|------|
| `ivan` | `ivan` | USER |
| `anna` | `anna` | USER, ADMIN |

Хеши сгенерированы `BCryptPasswordEncoder.encode(...)` и не эквивалентны
plaintext-паролям из таблицы: изменить пароль в конфиге «руками» нельзя,
нужно пересчитать BCrypt-хеш и подставить его в YAML.