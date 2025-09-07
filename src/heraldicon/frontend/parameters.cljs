(ns heraldicon.frontend.parameters
  (:require
   [clojure.string :as str]
   [re-frame.core :as rf]))

(def ^:private db-path
  [:ui :parameters])

(rf/reg-sub ::data
  :<- [:get db-path]

  (fn [data _]
    data))

(defn- encode-value [v]
  (cond
    (coll? v) (->> v
                   (map name)
                   sort
                   (str/join ","))
    (keyword? v) (name v)
    :else (str v)))

(defn- encode-param [k v]
  (str (js/encodeURIComponent (name k))
       "="
       (-> (js/encodeURIComponent (encode-value v))
           (str/replace "%20" "+")
           (str/replace "%2C" ","))))

(defn- map->query-string [m]
  (->> (sort-by key m)
       (keep (fn [[k v]]
               (when (and v
                          (or
                           (and (seqable? v)
                                (seq v))
                           (keyword? v)
                           (boolean? v)
                           (and (string? v)
                                (not (str/blank? v)))))
                 (encode-param k v))))
       (str/join "&")))

(defn- update-url! [m]
  (let [query (map->query-string m)
        new-url (str js/window.location.pathname
                     (when (seq query) (str "?" query)))]
    (.replaceState js/window.history #js {} "" new-url)))

(rf/reg-fx ::set-url-parameters
  (fn [data]
    (update-url! data)))

(rf/reg-event-fx ::set
  (fn [{:keys [db]} [_ data]]
    {:db (assoc-in db db-path data)
     ::set-url-parameters data}))

(defn query-string->map [s]
  (->> (str/split (or s "") #"&")
       (remove str/blank?)
       (map #(str/split % #"=" 2))
       (map (fn [[k v]]
              (let [key (keyword (js/decodeURIComponent k))
                    value (js/decodeURIComponent (or v ""))
                    value (cond
                            (= key :q) (str/replace value "+" " ")
                            (= key :tags) (->> (str/split value ",")
                                               (keep (fn [t]
                                                       (when-not (str/blank? v)
                                                         [(keyword t) true])))
                                               (into {}))
                            (= key :favorites) (= value "true")
                            :else (when-not (str/blank? value)
                                    (keyword value)))]
                (when value
                  [key value]))))
       (into {})))
