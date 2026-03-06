# 轮播图与文件上传接口文档

更新时间：2026-03-06

## 1. 轮播图接口（管理端）

接口前缀：`/api/admin/banner`

### 1.1 新增

- Method: `POST`
- Path: `/api/admin/banner`

请求体：

```json
{
  "image": "/upload/banner-001.png"
}
```

### 1.2 修改

- Method: `PUT`
- Path: `/api/admin/banner/{id}`

### 1.3 删除

- Method: `DELETE`
- Path: `/api/admin/banner/{id}`

### 1.4 详情

- Method: `GET`
- Path: `/api/admin/banner/{id}`

### 1.5 列表

- Method: `GET`
- Path: `/api/admin/banner/list`

## 2. 文件上传接口（管理端）

接口前缀：`/api/admin/file`

### 2.1 上传文件

- Method: `POST`
- Path: `/api/admin/file/upload`
- Content-Type: `multipart/form-data`
- 参数：`file`

返回示例：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "fileName": "a4e5c1f2.png",
    "path": "/www/wwwroot/upload/a4e5c1f2.png",
    "relativePath": "/upload/a4e5c1f2.png"
  }
}
```

前端建议：

- 将 `relativePath` 存为 banner 的 `image` 字段值。
