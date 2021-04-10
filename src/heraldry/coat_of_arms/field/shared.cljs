(ns heraldry.coat-of-arms.field.shared
  (:require [clojure.string :as s]
            [heraldry.coat-of-arms.field.environment :as environment]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.util :as util]))

(def overlap-stroke-width 0.1)

(defn get-field [fields index]
  (let [part (get fields index)]
    (case (:type part)
      :heraldry.field.type/ref (get fields (:index part))
      part)))

(defn field-context-key [key]
  (keyword (str "field-" (name key))))

(defn make-subfields [type fields parts mask-overlaps parent-environment parent
                      {:keys [render-field db-path svg-export?] :as context}]
  (let [mask-ids     (->> (range (count fields))
                          (map (fn [idx] [(util/id (str (name type) "-" idx))
                                          (util/id (str (name type) "-" idx))])))
        environments (->> parts
                          (map-indexed (fn [idx [shape-path bounding-box & extra]]
                                         (let [field (get-field fields idx)]
                                           (environment/create
                                            (svg/make-path shape-path)
                                            {:parent               parent
                                             :parent-environment   parent-environment
                                             :context              [type idx]
                                             :bounding-box         (svg/bounding-box bounding-box)
                                             :override-environment (when (or (:inherit-environment? field)
                                                                             (:counterchanged? field))
                                                                     parent-environment)
                                             :mask                 (first extra)}))))
                          vec)]
    [:<>
     [:defs
      (for [[idx [clip-path-id mask-id]] (map-indexed vector mask-ids)]
        (let [env               (get environments idx)
              environment-shape (:shape env)
              overlap-paths     (get mask-overlaps idx)]
          ^{:key idx}
          [:<>
           [(if svg-export?
              :mask
              :clipPath) {:id clip-path-id}
            [:path {:d    environment-shape
                    :fill "#fff"}]
            (cond
              (= overlap-paths :all) [:path {:d            environment-shape
                                             :fill         "none"
                                             :stroke-width overlap-stroke-width
                                             :stroke       "#fff"}]
              overlap-paths          (for [[idx shape] (map-indexed vector overlap-paths)]
                                       ^{:key idx}
                                       [:path {:d            shape
                                               :fill         "none"
                                               :stroke-width overlap-stroke-width
                                               :stroke       "#fff"}]))]
           (when-let [mask-shape (-> env :meta :mask)]
             [:mask {:id mask-id}
              [:path {:d    environment-shape
                      :fill "#fff"}]
              [:path {:d    mask-shape
                      :fill "#000"}]])]))]

     (for [[idx [clip-path-id mask-id]] (map-indexed vector mask-ids)]
       (let [env (get environments idx)]
         ^{:key idx}
         [:g {(if svg-export?
                :mask
                :clip-path) (str "url(#" clip-path-id ")")}
          [:g {:mask (when (-> env :meta :mask)
                       (str "url(#" mask-id ")"))}
           [render-field
            (get-field fields idx)
            (get environments idx)
            (-> context
                (assoc :db-path (if (-> type
                                        name
                                        (s/split #"-" 2)
                                        first
                                        (->> (get #{"charge" "ordinary"}))) ;; FIXME: bit of a hack
                                  (conj db-path :field)
                                  (conj db-path :fields idx))))]]]))]))

