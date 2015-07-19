(ns entityexp.core
  (:require [entityexp.queries :as queries]
            [routegen.core :as routegen]
            [clojure.edn :as edn]
            [compojure.core :as compojure]
            [compojure.route :as route]
            [ring.util.response :as response]
            [ring.middleware.defaults :as defaults]
            [ring.middleware.reload :as reload]))

(compojure/defroutes base-routes
  (compojure/GET "/" []
                 (response/redirect "/index.html"))
  (compojure/GET "/p/:id" [id]
                 (with-out-str
                   (clojure.pprint/pprint
                    (queries/pull-id (Long/parseLong id)))))
  (compojure/POST "/q" req
                  (let [q (edn/read-string (slurp (:body req)))]
                    (println "Q:" q)
                    (if (seq q)
                      (with-out-str
                        (clojure.pprint/pprint
                         (queries/q q)))
                      {:status 400
                       :body "No query specified"})))
  (route/resources "/")
  (route/not-found "not found"))

(def query-routes
  (apply compojure/routes
         (concat
          (routegen/post-routes 'entityexp.queries (comp edn/read-string slurp))
          (routegen/path-routes 'entityexp.queries))))

(def app-routes
  (compojure/routes query-routes base-routes))

(def handler
  (-> app-routes
      (defaults/wrap-defaults defaults/api-defaults)
      (reload/wrap-reload)))
