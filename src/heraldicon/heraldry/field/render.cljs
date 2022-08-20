(ns heraldicon.heraldry.field.render
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.render :as render]
   [heraldicon.heraldry.tincture :as tincture]
   [heraldicon.interface :as interface]
   [heraldicon.util.uid :as uid]))

(declare render)

(defn- effective-field-context [context]
  (let [field-type (interface/get-raw-data (c/++ context :type))]
    (if (= field-type :heraldry.field.type/ref)
      (let [index (interface/get-raw-data (c/++ context :index))
            source-context (-> context c/-- (c/++ index))]
        (assoc-in source-context [:path-map (:path source-context)] (:path context)))
      context)))

(defn- render-subfield [{:keys [svg-export?
                                charge-preview?]
                         :as context} transform]
  (let [clip-path-id (uid/generate "clip")
        subfield-context (effective-field-context context)]
    [:g
     [:defs
      [(if svg-export?
         :mask
         :clipPath) {:id clip-path-id}
       ;; TODO: edge overlap strategy
       [render/shape-mask subfield-context]]]
     [:g {(if svg-export?
            :mask
            :clip-path) (str "url(#" clip-path-id ")")}
      [:g {:style (when-not (or svg-export?
                                charge-preview?)
                    {:pointer-events "visiblePainted"
                     :cursor "pointer"})
           :transform transform}
       [render subfield-context]]]]))

(defn- render-subfields [context]
  (let [{:keys [num-subfields transform]} (interface/get-properties context)]
    (into [:g]
          (map (fn [idx]
                 [render-subfield (c/++ context :fields idx) transform]))
          (range num-subfields))))

(defn- field-path-allowed? [{:keys [path counterchanged-paths]}]
  (let [component-path (->> path
                            reverse
                            (drop-while (comp not int?)))
        index (first component-path)
        components-path (->> component-path
                             (drop 1)
                             reverse
                             vec)
        length (count components-path)]
    (loop [[counterchanged-path & rest] counterchanged-paths]
      (if (nil? counterchanged-path)
        true
        (let [counterchanged-path (vec counterchanged-path)
              start (when (-> counterchanged-path count (>= length))
                      (subvec counterchanged-path 0 length))]
          (if (and (= start components-path)
                   (>= index (get counterchanged-path length)))
            false
            (recur rest)))))))

(defn- render-components [context]
  (into [:<>]
        (for [idx (range (interface/get-list-size (c/++ context :components)))
              :while (field-path-allowed? (c/++ context :components idx))]
          ^{:key idx}
          [interface/render-component (c/++ context :components idx)])))

(defn render [context]
  (let [{:keys [render-fn]
         field-type :type
         :as properties} (interface/get-properties context)]
    [:<>
     (cond
       (= field-type :heraldry.field.type/plain) (tincture/tinctured-field context)
       (= field-type :heraldry.field.type/counterchanged) [:<>]
       ;; TODO: simplify once render-components doesn't have to be passed along anymore
       render-fn [render-fn context properties]
       :else [render-subfields context])

     [render/field-edges context]

     [render-components context]]))
