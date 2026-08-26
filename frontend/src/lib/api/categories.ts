import { client } from "./client";
import { CategoryRead, DataResponse } from "./expenses";

export interface CategoryCreate {
  name: string;
}

export interface CategoryUpdate {
  name: string;
}

export const categoriesApi = {
  list: () => client.get<DataResponse<CategoryRead[]>>("/categories"),
  
  get: (id: string) => client.get<DataResponse<CategoryRead>>(`/categories/${id}`),

  create: (data: CategoryCreate) => client.post<DataResponse<CategoryRead>>("/categories", data),

  update: (id: string, data: CategoryUpdate) => client.patch<DataResponse<CategoryRead>>(`/categories/${id}`, data),

  delete: (id: string) => client.delete<void>(`/categories/${id}`),
};
