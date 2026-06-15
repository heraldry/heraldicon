(ns heraldicon.frontend.repository.charge-types
  (:require
   [cljs.core.async :refer [go]]
   [com.wsscode.async.async-cljs :refer [<?]]
   [heraldicon.frontend.api :as api]
   [heraldicon.frontend.repository.core :as repository]
   [heraldicon.frontend.repository.query :as query]
   [heraldicon.frontend.user.session :as session]
   [heraldicon.heraldry.charge.options :as charge.options]
   [heraldicon.heraldry.facets :as facets]
   [re-frame.core :as rf]
   [taoensso.timbre :as log])
  (:require-macros [reagent.ratom :refer [reaction]]))

(def db-path-charge-types
  (conj repository/db-path-base :charge-types))

(def db-path-standard-shapes
  ;; Synthetic top-level entries used by the arms autocomplete tree to cover
  ;; the 10 inline geometric charges that don't necessarily have an entry in
  ;; the DB charge_types tree. Populated by ::store with only the subset of
  ;; shapes whose slug isn't already covered by the DB tree.
  (conj repository/db-path-base :charge-types-standard-shapes))

(def ^:private standard-shape-types
  ;; Derived from charge.options/charges — the actual list of inline charge
  ;; type keywords (`:heraldry.charge.type/roundel`, …). The :name is the
  ;; keyword's name suffix, which is exactly the slug the extractor emits
  ;; (`charge:roundel`) and what arms.facets stores. So picking one from the
  ;; tree, slugifying its name, and the existing facet:value indexing all
  ;; agree.
  (mapv (fn [kw]
          {:type :heraldicon/charge-type
           :name (name kw)
           :types []})
        charge.options/charges))

(defn- collect-slugs [data]
  (set (->> (tree-seq #(seq (:types %)) :types data)
            (keep :name)
            (map facets/slugify-name))))

(defn- missing-standard-shapes [data]
  (let [existing (collect-slugs data)]
    (filterv #(not (existing (facets/slugify-name (:name %))))
             standard-shape-types)))

(defn clear
  [db]
  (-> db
      (assoc-in db-path-charge-types nil)
      (assoc-in db-path-standard-shapes nil)))

(rf/reg-event-db ::store
  (fn [db [_ data]]
    (-> db
        (assoc-in db-path-charge-types {:status :done
                                        :data data
                                        :path (conj db-path-charge-types :data)})
        (assoc-in db-path-standard-shapes (missing-standard-shapes data)))))

(rf/reg-event-db ::store-error
  (fn [db [_ error]]
    (assoc-in db db-path-charge-types {:status :error
                                       :error error})))

(rf/reg-event-db ::clear
  (fn [db [_]]
    (clear db)))

(defn- fetch [session on-fetch]
  (go
    (let [query-id [::fetch session]]
      (when-not (query/running? query-id)
        (query/register query-id)
        (try
          (let [data (<? (api/call :fetch-relevant-charge-type-data {} session))]
            (when on-fetch
              (on-fetch data))
            (rf/dispatch [::store data]))
          (catch :default e
            (log/error e "fetch charge-type data error")
            (rf/dispatch [::store-error e]))
          (finally
            (query/unregister query-id)))))))

(rf/reg-sub-raw ::data
  (fn [_app-db [_ on-fetch]]
    (reaction
     (let [session @(rf/subscribe [::session/data])]
       (repository/async-query-data db-path-charge-types (partial fetch session on-fetch))))))
