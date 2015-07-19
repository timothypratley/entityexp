(ns ^:figwheel-always entityexp.main
    (:require [cljs.core.async :refer [<!]]
              [cljs-http.client :as http]
              [cljsjs.react :as react]
              [clojure.string :as string]
              [goog.dom.forms :as forms]
              [reagent.core :as reagent])
    (:require-macros [cljs.core.async.macros :refer [go]]))

(enable-console-print!)

(defonce app-state (reagent/atom {}))
(defonce ins (reagent/atom #{}))
(def base-url "http://localhost:3000/")

(def q-s
  '[:find ?attr ?type ?card
    :where
    [_ :db.install/attribute ?a]
    [?a :db/valueType ?t]
    [?a :db/cardinality ?c]
    [?a :db/ident ?attr]
    [?t :db/ident ?type]
    [?c :db/ident ?card]])

(def q-ident
  '[:find ?i
    :in $
    :where [$ _ :db/ident ?i]])

(defn q-in [id]
  [:find '(pull ?e [(limit * 10)])
   :in '$
   :where ['$ '?e '?a id]])

(def q
  '[:find ?e
    :in $
    :where [$ ?e :artist/name "John Lennon"]])

(defn q-id [id]
  [:find '(pull ?e [*])
   :in '$
   :where ['$ '?e :db/id id]])

(defn get-em [q]
  (go
    (reset! app-state
            (let [{:keys [body success error-text]}
                  (<! (http/post (str base-url "q")
                                 {:edn-params q}))]
              (if success
                (cljs.reader/read-string body)
                (println error-text))))))

(defn get-em-all2 [id]
  (go
    (reset!
     app-state
     (let [{:keys [body success error-text]}
           (<! (http/post (str base-url "q")
                          {:edn-params (q-id id)}))]
       (if success
         (cljs.reader/read-string body)
         (println error-text))))))

;; TODO: is this just pull '[* *] ?
;; (limit * 10) ?
(defn get-em-all [id]
  (go
    (reset! app-state
            (let [{:keys [body success error-text]}
                  (<! (http/get (str base-url "p/" id)))]
              (if success
                (cljs.reader/read-string body)
                (println error-text))))
    (println @app-state)
    (reset! ins
            (let [{:keys [body success error-text]}
                  (<! (http/post (str base-url "q")
                                 {:edn-params (q-in id)}))]
              (if success
                (cljs.reader/read-string body)
                (println error-text))))
    (println @ins)))

(get-em-all 17592186050247)

(declare render)

(defn render-map [m depth]
  (let [n (or (first (filter (comp #{"name"} name) (keys m)))
              (first (filter #{:db/ident} (keys m)))
              (first (filter #{:db/id} (keys m))))]
    (into [:ul.list-unstyled
           (when n
             [:div
              (if (zero? depth)
                [(keyword (str "h" (+ depth 2)))
                 [:div.row
                  [:div.col-xs-4 (string/capitalize (namespace n))]
                  [:div.col-xs-8 (render (get m n)) [:br] [:small (:db/id m)]]]
                 [:hr]]
                [:a {:on-click (fn [e]
                                 (get-em-all (:db/id m)))}
                 (render (get m n))])])]
          (for [[k v] (sort-by (comp name key) (dissoc m :db/id n :db/ident))]
            [:li.row
             [:div.col-xs-4 {:style {:text-align "right"}}
              [:b (render k)]]
             [:div.col-xs-8 (render v (inc depth))]]))))

(defn render
  ([x] (render x 0))
  ([x depth]
   (cond
     (map? x) (render-map x depth)
     (string? x) x
     (or (vector? x) (set? x) (sequential? x)) (into [:ul.list-unstyled.well]
                                                     (for [y x]
                                                       [:li.row
                                                        (render y (inc depth))]))
     (keyword? x) (name x)
     :else x)))

(defn form-data
  "Returns a kewordized map of forms input name, value pairs."
  [e]
  (.preventDefault e)
  (into {}
        (for [[k v] (js->clj (.toObject (forms/getFormDataMap (.-target e))))]
          [(keyword k) (if (<= (count v) 1)
                         (first v)
                         v)])))

(defn current-page []
  [:div.container
   [:h1 "Entity explorer"]
   [:div
    [:form.form-inline
     {:on-submit (fn [e]
                   (get-em-all (:id (form-data e))))}
     [:form-group
      [:label "ID"]
      [:input {:type "text"
               :name "id"}]]
     [:input {:type "submit"}]]
    [:form.form-inline
     {:on-submit (fn [e]
                   (get-em (:q (form-data e))))}
     [:form-group
      [:label "Q"]
      [:input {:type "text"
               :name "q"}]]
     [:input {:type "submit"}]]
    [:button.btn.btn-default
     {:on-click (fn [e]
                  (get-em-all 17592186050247))}
     "Get John"]
    [:button.btn.btn-default
     {:on-click (fn [e]
                  (get-em '[:find ?title
                            :where
                            [?a :artist/name "John Lennon"]
                            [?t :track/artists ?a]
                            [?t :track/name ?title]]))}
     "Get Titles"]
    [:button.btn.btn-default
     {:on-click (fn [e]
                  (get-em '[:find (pull ?t [* {:track/artists [:db/id :artist/name]
                                               :artist/type [:db/id :db/ident]
                                               :artist/country [:db/id :country/name]
                                               :artist/gender [:db/id :db/ident]}])
                            :where
                            [?a :artist/name "John Lennon"]
                            [?t :track/artists ?a]]))}
     "Get Tracks"]
    [:button.btn.btn-default
     {:on-click (fn [e]
                  (get-em q-ident))}
     "Get Idents"]
    [:button.btn.btn-default
     {:on-click (fn [e]
                  (get-em q-s))}
     "Get Schema"]]
   [:div.well
    [:span (render @app-state)]]
   [:div.well
    [:span (render @ins)]]
   [:div.well
    [:div {:id "disqus_thread"}]]])

(defn mount-root []
  (println "Reloaded")
  (reagent/render [#'current-page] (.getElementById js/document "app")))

(defn init! []
  (mount-root))

(defonce start
  (init!))
