-- Bucket público de lectura para las imágenes de SportPro. No se crean políticas sobre
-- storage.objects a propósito: nadie puede listar, subir ni borrar con las claves públicas.
-- Solo escribe la Edge Function "imagenes", que valida el token de Firebase Auth.
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('imagenes', 'imagenes', true, 5242880, array['image/jpeg'])
on conflict (id) do update
  set public = excluded.public,
      file_size_limit = excluded.file_size_limit,
      allowed_mime_types = excluded.allowed_mime_types;
