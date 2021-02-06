(ns heraldry.frontend.charge-map
  (:require [cljs.core.async :refer [go]]
            [clojure.set :as set]
            [clojure.walk :as walk]
            [com.wsscode.common.async-cljs :refer [<?]]
            [heraldry.api.request :as api-request]
            [heraldry.frontend.http :as http]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.user :as user]))

(defn fetch-charge-map []
  (go
    (try
      (let [user-data (user/data)]
        (<? (api-request/call :get-charge-map {} user-data)))
      (catch :default e
        (println "fetch-charge-map error:" e)))))

(defn fetch-charge [id version]
  (go
    (try
      (let [user-data (user/data)
            response (<? (api-request/call :fetch-charge {:id id
                                                          :version version} user-data))
            edn-data (<? (http/fetch (:edn-data-url response)))]
        (-> response
            (assoc :data edn-data)))
      (catch :default e
        (println "fetch-charge error:" e)))))

(defn get-charge-map []
  (let [db-path [:charge-map]
        [status charge-map] (state/async-fetch-data
                             db-path
                             :map
                             fetch-charge-map)]
    (when (= status :done)
      charge-map)))

(defn fetch-charge-data [{:keys [id version] :as variant}]
  (if (and id version)
    (let [db-path [:charge-data variant]
          [status charge-data] (state/async-fetch-data
                                db-path
                                variant
                                #(fetch-charge id version))]
      (when (= status :done)
        charge-data))
    (println "error fetching charge data, variant:" variant)))

(def group-map
  {:node-type :_root
   :groups {:beasts {:node-type :group
                     :name "beasts"
                     :groups {:predators {:node-type :group
                                          :name "predators"
                                          :charges #{:lion :wolf :bear}}
                              :ungulates {:node-type :group
                                          :name "ungulates"
                                          :charges #{:antelope :deer :boar}}
                              :reptiles {:node-type :group
                                         :name "reptiles"
                                         :charges #{:lizard :serpent}}
                              :insects {:node-type :group
                                        :name "insects"
                                        :charges #{:bee :butterfly}}
                              :hybrids {:node-type :group
                                        :name "hybrids"
                                        :charges #{:sphinx :griffin :unicorn}}
                              :birds {:node-type :group
                                      :name "birds"
                                      :charges #{:dove :crane :eagle :owl :corvus}}}}}})

(def known-charge-types
  (walk/postwalk (fn [data]
                   (cond
                     (:charges data) (:charges data)
                     (:groups data) (:groups data)
                     (map? data) (->> data
                                      (map second)
                                      (apply set/union))
                     :else data)) group-map))

(defn build-charge-variants [node groupable-types charges]
  (let [type (first groupable-types)
        remaining-types (drop 1 groupable-types)
        groups (if type
                 (group-by type charges)
                 {})
        nil-group (if type
                    (get groups nil)
                    charges)
        non-nil-groups (when type
                         (into {}
                               (filter first groups)))]
    (-> node
        (cond->
         non-nil-groups (assoc (-> type
                                   name
                                   (str "s")
                                   keyword)
                               (->> non-nil-groups
                                    (map (fn [[value grouped-charges]]
                                           [value (build-charge-variants
                                                   {:node-type type
                                                    :type value
                                                    :name (name value)}
                                                   remaining-types
                                                   grouped-charges)]))
                                    (into {})))
         (and nil-group
              (-> remaining-types
                  seq)) (build-charge-variants remaining-types nil-group)
         (and nil-group
              (-> remaining-types
                  empty?)) (assoc :variants (->> nil-group
                                                 (map (fn [charge]
                                                        [(:id charge) {:node-type :variant
                                                                       :name (:name charge)
                                                                       :data charge}]))
                                                 (into {})))))))

(defn build-map [group-map charges-by-type]
  (let [charge-map (walk/postwalk
                    (fn [data]
                      (if (and (-> data seqable?)
                               (-> data first (= :charges)))
                        (let [charge-types (second data)]
                          [:charges (->> charge-types
                                         (map (fn [type]
                                                [type (build-charge-variants
                                                       {:node-type :charge
                                                        :type type
                                                        :name (name type)}
                                                       [:attitude]
                                                       (get charges-by-type type))]))

                                         (into {}))])
                        data)) group-map)
        ungrouped-types (-> charges-by-type
                            keys
                            set
                            (set/difference known-charge-types))]
    (cond-> charge-map
      (seq ungrouped-types) (assoc-in [:groups :ungrouped]
                                      {:node-type :group
                                       :name "ungrouped"
                                       :charges (->> ungrouped-types
                                                     (map (fn [type]
                                                            [type (build-charge-variants
                                                                   {:node-type :charge
                                                                    :type type
                                                                    :name (name type)}
                                                                   [:attitude]
                                                                   (get charges-by-type type))]))
                                                     (into {}))}))))

(defn build-charge-map [charges]
  (->> charges
       (group-by :type)
       (build-map group-map)))
