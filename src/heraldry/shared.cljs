(ns heraldry.shared
  (:require [heraldry.attribution :as attribution]
            [heraldry.coat-of-arms.attributes :as attributes]
            [heraldry.coat-of-arms.charge-group.core] ;; needed for side effects
            [heraldry.coat-of-arms.charge-group.options] ;; needed for side effects
            [heraldry.coat-of-arms.charge.core] ;; needed for side effects
            [heraldry.coat-of-arms.charge.options] ;; needed for side effects
            [heraldry.coat-of-arms.charge.other] ;; needed for side effects
            [heraldry.coat-of-arms.core] ;; needed for side effects
            [heraldry.coat-of-arms.counterchange :as counterchange]
            [heraldry.coat-of-arms.field.core] ;; needed for side effects
            [heraldry.coat-of-arms.field.shared] ;; needed for side effects
            [heraldry.coat-of-arms.ordinary.core] ;; needed for side effects
            [heraldry.coat-of-arms.ordinary.options] ;; needed for side effects
            [heraldry.coat-of-arms.semy.core] ;; needed for side effects
            [heraldry.coat-of-arms.semy.options] ;; needed for side effects
            [heraldry.coat-of-arms.tincture.core :as tincture]
            [heraldry.collection.element] ;; needed for side effects
            [heraldry.collection.options] ;; needed for side effects
            [heraldry.font :as font]
            [heraldry.interface :as interface]
            [heraldry.options :as options]
            [heraldry.render-options] ;; needed for side effects
            ))

(defmethod interface/get-raw-data :context [path context]
  (get-in context (drop 1 path)))

(defmethod interface/get-list-size :context [path context]
  (count (get-in context (drop 1 path))))

(defmethod interface/get-counterchange-tinctures :context [path context]
  (-> (interface/get-raw-data path context)
      (counterchange/get-counterchange-tinctures context)))

(defn get-options-by-context [path context]
  (interface/component-options path (interface/get-raw-data path context)))

(defn get-relevant-options-by-context [path context]
  (let [[options relative-path] (or (->> (range (count path) 0 -1)
                                         (keep (fn [idx]
                                                 (let [option-path (subvec path 0 idx)
                                                       relative-path (subvec path idx)
                                                       options (get-options-by-context option-path context)]
                                                   (when options
                                                     [options relative-path]))))
                                         first)
                                    [nil nil])]
    (get-in options relative-path)))

(defmethod interface/get-sanitized-data :context [path context]
  (let [data (interface/get-raw-data path context)
        options (get-relevant-options-by-context path context)]
    (options/sanitize-value-or-data data options)))

;; TODO: might not be the right place for it, others live in the coat-of-arms.[thing].options namespaces
(defmethod interface/component-options :heraldry.options/arms-general [_path data]
  {:name {:type :text
          :default ""
          :ui {:label "Name"}}
   :is-public {:type :boolean
               :ui {:label "Make public"}}
   :attribution (attribution/options (:attribution data))
   :tags {:ui {:form-type :tags}}})

;; TODO: might not be the right place for it, others live in the coat-of-collection.[thing].options namespaces
(defmethod interface/component-options :heraldry.options/collection-general [_path _data]
  {:name {:type :text
          :default ""
          :ui {:label "Name"}}
   :is-public {:type :boolean
               :ui {:label "Make public"}}
   :attribution attribution/default-options
   :tags {:ui {:form-type :tags}}
   :font font/default-options})

;; TODO: might not be the right place for it, others live in the coat-of-charge.[thing].options namespaces
(defmethod interface/component-options :heraldry.options/charge-general [_path data]
  {:name {:type :text
          :default ""
          :ui {:label "Name"}}
   :is-public {:type :boolean
               :ui {:label "Make public"}}
   :attribution (attribution/options (:attribution data))
   :tags {:ui {:form-type :tags}}
   :type {:type :text
          :ui {:label "Charge type"}}
   :attitude {:type :choice
              :choices attributes/attitude-choices
              :default :none
              :ui {:label "Attitude"}}
   :facing {:type :choice
            :choices attributes/facing-choices
            :default :none
            :ui {:label "Facing"}}
   :colours {:ui {:form-type :colours}}
   :attributes {:ui {:form-type :attributes}}
   :fixed-tincture {:type :choice
                    :choices tincture/fixed-tincture-choices
                    :default :none
                    :ui {:label "Fixed tincture"}}})
