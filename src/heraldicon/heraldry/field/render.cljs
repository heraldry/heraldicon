(ns heraldicon.heraldry.field.render
  (:require
   [heraldicon.context :as c]
   [heraldicon.frontend.counterchange :as counterchange]
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
                         :as context} transform overlap?]
  (let [clip-path-id (uid/generate "clip")
        subfield-context (effective-field-context context)]
    [:g
     [:defs
      [(if svg-export?
         :mask
         :clipPath) {:id clip-path-id}
       [render/shape-mask subfield-context overlap?]]]
     [:g {(if svg-export?
            :mask
            :clip-path) (str "url(#" clip-path-id ")")}
      [:g {:style (when-not (or svg-export?
                                charge-preview?)
                    {:pointer-events "visiblePainted"
                     :cursor "pointer"})
           :transform transform}
       [render subfield-context]]]]))

(defn- render-subfields [context {:keys [num-subfields transform overlap?-fn]
                                  :or {overlap?-fn even?}}]
  (into [:g]
        (map (fn [idx]
               ^{:key idx}
               [render-subfield (c/++ context :fields idx) transform (overlap?-fn idx)]))
        (sort-by overlap?-fn > (range num-subfields))))

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

(defn- add-tinctures-to-mapping [context counterchange-tinctures]
  (if (-> counterchange-tinctures count (= 2))
    (let [[t1 t2] counterchange-tinctures
          tincture-replacer {t1 t2
                             t2 t1}]
      (update context
              :tincture-mapping
              (fn [tincture-mapping]
                (let [new-mapping (into {}
                                        (map (fn [[k v]]
                                               [k (get tincture-replacer v v)]))
                                        tincture-mapping)]
                  (cond-> new-mapping
                    (not (contains? new-mapping t1)) (assoc t1 t2)
                    (not (contains? new-mapping t2)) (assoc t2 t1))))))
    context))

(declare render)

(defn- render-counterchanged-field [{:keys [path]
                                     :as context} _properties]
  (when-let [parent-field-context (some-> context interface/parent interface/parent)]
    (let [counterchange-tinctures (counterchange/tinctures parent-field-context)
          counterchanged-context (-> parent-field-context
                                     (update :counterchanged-paths conj path)
                                     (add-tinctures-to-mapping counterchange-tinctures))]
      [render counterchanged-context])))

(defn- render-plain-field [context _properties]
  [tincture/tinctured-field context])

(defn render [context]
  (let [{:keys [render-fn]
         field-type :type
         :as properties} (interface/get-properties context)
        render-fn (case field-type
                    :heraldry.field.type/plain render-plain-field
                    :heraldry.field.type/counterchanged render-counterchanged-field
                    (or render-fn
                        render-subfields))]
    [:<>
     [render-fn context properties]
     [render/field-edges context]
     [render-components context]]))
