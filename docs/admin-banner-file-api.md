# Banner与文件上传接口文档（前端联调）

更新时间：2026-03-05

## 1. Banner CRUD（管理员）

接口前缀：`/api/admin/banner`

### 1.1 新增 Banner

- `POST /api/admin/banner`

请求体：

```json
{
  "image": "/upload/xxx.png"
}
```

### 1.2 修改 Banner

- `PUT /api/admin/banner/{id}`

请求体：

```json
{
  "image": "/upload/xxx-new.png"
}
```

### 1.3 删除 Banner

- `DELETE /api/admin/banner/{id}`

### 1.4 Banner 详情

- `GET /api/admin/banner/{id}`

### 1.5 Banner 列表

- `GET /api/admin/banner/list`

返回示例：

```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "id": 3,
      "image": "/upload/xxx.png",
      "createTime": "2026-03-05T10:00:00.000+00:00",
      "updateTime": "2026-03-05T10:00:00.000+00:00"
    }
  ]
}
```

---

## 2. 文件上传（管理员）

接口前缀：`/api/admin/file`

### 2.1 上传文件

- `POST /api/admin/file/upload`
- `Content-Type: multipart/form-data`
- 参数：`file`（文件）
- 上传目录：`/www/wwwroot/upload`

成功返回：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "fileName": "a4e5c1f2....png",
    "path": "/www/wwwroot/upload/a4e5c1f2....png",
    "relativePath": "/upload/a4e5c1f2....png"
  }
}
```

前端建议：上传成功后取 `relativePath` 作为 `image` 字段，传给 Banner 新增/修改接口。
