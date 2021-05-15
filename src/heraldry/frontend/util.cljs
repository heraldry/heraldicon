(ns heraldry.frontend.util
  (:require [clojure.string :as s]
            [clojure.walk :as walk]
            [heraldry.config :as config]
            [reitit.frontend.easy :as reife]))

(defn lower-case-first [s]
  (str (s/lower-case (or (first s) "")) (s/join (rest s))))

(defn upper-case-first [s]
  (str (s/upper-case (or (first s) "")) (s/join (rest s))))

(defn translate [keyword]
  (when keyword
    (-> keyword
        name
        (s/replace "-" " ")
        (s/replace "fleur de lis" "fleur-de-lis")
        (s/replace "fleur de lys" "fleur-de-lys"))))

(defn translate-tincture [keyword]
  (case keyword
    :none "[no tincture]"
    (translate keyword)))

(defn translate-line [{:keys [type]}]
  (when (not= type :straight)
    (translate type)))

(defn translate-cap-first [keyword]
  (-> keyword
      translate
      upper-case-first))

(defn combine [separator words]
  (->> words
       (map (fn [s]
              (if (string? s)
                s
                (str s))))
       (filter #(> (count %) 0))
       (s/join separator)))

(defn contains-in?
  [m ks]
  (not= ::absent (get-in m ks ::absent)))

(defn replace-recursively [data value replacement]
  (walk/postwalk #(if (= % value)
                    replacement
                    %)
                 data))

(defn id-for-url [id]
  (-> id
      (s/split #":" 2)
      second))

(defn full-url-for-arms [arms-data]
  (let [version (:version arms-data)
        version (if (zero? version)
                  (:latest-version arms-data)
                  version)
        arms-id (-> arms-data
                    :id
                    id-for-url)]
    (str (config/get :heraldry-url) (reife/href :view-arms-by-id-and-version {:id arms-id
                                                                              :version version}))))

(defn full-url-for-charge [charge-data]
  (let [version (:version charge-data)
        version (if (zero? version)
                  (:latest-version charge-data)
                  version)
        charge-id (-> charge-data
                      :id
                      id-for-url)]

    (str (config/get :heraldry-url) (reife/href :view-charge-by-id-and-version {:id charge-id
                                                                                :version version}))))
