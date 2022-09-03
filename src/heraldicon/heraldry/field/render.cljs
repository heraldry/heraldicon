(ns heraldicon.heraldry.field.render
  (:require
   [heraldicon.context :as c]
   [heraldicon.frontend.counterchange :as counterchange]
   [heraldicon.heraldry.render :as render]
   [heraldicon.heraldry.subfield :as subfield]
   [heraldicon.heraldry.tincture :as tincture]
   [heraldicon.interface :as interface]))

(defn- render-subfields [context {:keys [num-subfields transform overlap?-fn]
                                  :or {overlap?-fn even?}}]
  ;; TODO: overlap should move into subfield/render
  (into [:g]
        (map (fn [idx]
               ^{:key idx}
               [subfield/render (c/++ context :fields idx) transform (overlap?-fn idx)]))
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

(defn- render-counterchanged-field [{:keys [path]
                                     :as context} _properties]
  (when-let [parent-field-context (interface/get-counterchange-parent (interface/parent context))]
    (let [counterchange-tinctures (counterchange/tinctures parent-field-context)
          counterchanged-context (-> parent-field-context
                                     (update :counterchanged-paths conj path)
                                     (add-tinctures-to-mapping counterchange-tinctures))]
      [interface/render-component counterchanged-context])))

(defn- render-plain-field [context _properties]
  [tincture/tinctured-field context])

(defmethod interface/render-component :heraldry/field [context]
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
