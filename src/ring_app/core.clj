(ns ring-app.core
  (:require [reitit.ring :as reitit]
            [ring.adapter.jetty :as jetty]
            [ring.util.http-response :as response]
            [ring.middleware.reload :refer [wrap-reload]]
            [muuntaja.middleware :refer [wrap-format]]))

(defn wrap-nocache [handler]
  (fn [request]
    (-> request
        handler
        (assoc-in [:headers "Pragma"] "no-cache"))))

(defn response-handler [request]
  (response/ok (str "<html><body> your IP is: " (:remote-addr request) "</body></html>")))

(defn echo-handler [{{:keys [value]} :path-params}]
  (response/ok (str "<html><body><p> ECHO: " value "</p></body></html>")))

(defn api-multiply-handler [{{:keys [a b]} :body-params}]
  (response/ok {:result (* a b)}))

(def routes
  [["/" {:get response-handler
         :post response-handler}]
   ["/echo/:value" {:get echo-handler}]
   ["/api" {:middleware [wrap-format]}
    ["/multiply" {:post api-multiply-handler}]]])

(def handler
  (reitit/routes
    (reitit/ring-handler
      (reitit/router routes))
    (reitit/create-resource-handler
      {:path "/"})
    (reitit/create-default-handler
      {:not-found (constantly (response/not-found "404 - Page Not Found"))
       :method-not-allowed (constantly (response/method-not-allowed "405 - Not Allowed"))
       :not-acceptable (constantly (response/not-acceptable "406 - Not Acceptable"))})))

(defn -main []
  (jetty/run-jetty
    ; #'handler is a way of assigning handler to a variable so it can be passed into each of the middlewares
    ; without it only the original handler would be returned and you wouldn't get any middlewares to work
    (-> #'handler
        wrap-nocache
        wrap-reload)
    {:port 3000
     :join? false}))
