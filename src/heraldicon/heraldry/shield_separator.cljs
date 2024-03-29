(ns heraldicon.heraldry.shield-separator
  (:require
   [heraldicon.heraldry.default :as default]
   [heraldicon.interface :as interface]
   [re-frame.core :as rf]))

(defn shield-separator? [element]
  (-> element
      :type
      (= :heraldry/shield-separator)))

(def add-element-insert-at-bottom-options
  {:post-fn (fn [db path]
              (update-in db path (fn [elements]
                                   (let [elements (into [(last elements)]
                                                        (drop-last elements))]
                                     (if (-> elements count (= 1))
                                       (into elements
                                             [default/shield-separator])
                                       elements)))))
   :selected-element-path-fn (fn [selected-path _element _elements]
                               (-> selected-path
                                   drop-last
                                   vec
                                   (conj 0)))})

(defn add-or-remove-shield-separator
  [db path]
  (let [path (if (int? (last path))
               (drop-last path)
               path)]
    (update-in db path (fn [elements]
                         (let [elements (if (->> elements
                                                 (filter (comp not shield-separator?))
                                                 count
                                                 zero?)
                                          []
                                          elements)]
                           (if (-> elements count (= 1))
                             (into [default/shield-separator]
                                   elements)
                             elements))))))

(defn- shield-separator-exists? [elements]
  (->> elements
       (filter shield-separator?)
       seq))

(defn- element-indices-below-shield [elements]
  (if (shield-separator-exists? elements)
    (->> elements
         (map-indexed vector)
         (take-while (comp not shield-separator? second))
         (map first)
         vec)
    []))

(defn- element-indices-above-shield [elements]
  (if (shield-separator-exists? elements)
    (->> elements
         (map-indexed vector)
         (drop-while (comp not shield-separator? second))
         (filter (comp not shield-separator? second))
         (map first)
         vec)
    (->> (count elements)
         range
         vec)))

(defn element-indices-with-position [elements]
  (vec (concat (map (fn [idx]
                      [idx true]) (element-indices-below-shield elements))
               (map (fn [idx]
                      [idx false]) (element-indices-above-shield elements)))))

;; TODO: get rid of this
(defn get-element-indices [{:keys [path]}]
  (element-indices-with-position @(rf/subscribe [:get path])))

(defmethod interface/properties :heraldry/shield-separator [_context]
  {:type :heraldry/shield-separator})

(defmethod interface/bounding-box :heraldry/shield-separator [_context]
  nil)

(defmethod interface/render-component :heraldry/shield-separator [_context]
  nil)
