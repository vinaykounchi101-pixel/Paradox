import { client } from "./client";
import { PaymentMethodRead, DataResponse } from "./expenses";

export interface PaymentMethodCreate {
  name: string;
}

export interface PaymentMethodUpdate {
  name: string;
}

export const paymentMethodsApi = {
  list: () => client.get<DataResponse<PaymentMethodRead[]>>("/payment-methods"),

  create: (data: PaymentMethodCreate) => client.post<DataResponse<PaymentMethodRead>>("/payment-methods", data),

  update: (id: string, data: PaymentMethodUpdate) => client.patch<DataResponse<PaymentMethodRead>>(`/payment-methods/${id}`, data),

  delete: (id: string) => client.delete<void>(`/payment-methods/${id}`),
};
