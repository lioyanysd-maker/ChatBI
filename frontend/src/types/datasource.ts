export interface DataSource {
  id: number
  name: string
  dbType: string
  host: string
  port: number
  databaseName: string
  username: string
  status: number
  createdAt?: string
}

export interface DataSourceForm {
  name: string
  dbType: string
  host: string
  port: number
  databaseName: string
  username: string
  password: string
}

export interface TableWhitelistItem {
  id: number
  tableName: string
  tableComment?: string
  active: boolean
}
