(ns heraldry.interface
  (:require [clojure.string :as s]
            [taoensso.timbre :as log]))

(defmulti component-options
  (fn [path data]
    (cond
      (-> path last
          (= :arms-form)) :heraldry.options/arms-general
      (-> path last
          (= :charge-form)) :heraldry.options/charge-general
      (-> path last
          (= :collection-form)) :heraldry.options/collection-general
      (-> path last
          (= :collection)) :heraldry.options/collection
      (-> path drop-last
          (->> (take-last 2))
          (= [:collection :elements])) :heraldry.options/collection-element
      (-> path last
          (= :render-options)) :heraldry.options/render-options
      (-> path last
          (= :coat-of-arms)) :heraldry.options/coat-of-arms
      (-> path drop-last last
          (= :strips)) :heraldry.options/charge-group-strip
      :else (let [ts (-> data :type str)]
              (cond
                (s/starts-with? ts ":heraldry.field") :heraldry.options/field
                (s/starts-with? ts ":heraldry.ordinary") :heraldry.options/ordinary
                (s/starts-with? ts ":heraldry.component/charge-group-strip") :heraldry.options/charge-group-strip
                (s/starts-with? ts ":heraldry.charge-group") :heraldry.options/charge-group
                (s/starts-with? ts ":heraldry.charge") :heraldry.options/charge
                (s/starts-with? ts ":heraldry.component/semy") :heraldry.options/semy
                :else nil)))))

(defmethod component-options nil [_path _data]
  nil)

(defn type->component-type [t]
  (let [ts (str t)]
    (cond
      (s/starts-with? ts ":heraldry.component") t
      (s/starts-with? ts ":heraldry.field") :heraldry.component/field
      (s/starts-with? ts ":heraldry.ordinary") :heraldry.component/ordinary
      (s/starts-with? ts ":heraldry.charge-group") :heraldry.component/charge-group
      (s/starts-with? ts ":heraldry.charge") :heraldry.component/charge
      :else nil)))

(defn effective-component-type [path raw-type]
  (cond
    (-> path last (= :arms-form)) :heraldry.component/arms-general
    (-> path last (= :charge-form)) :heraldry.component/charge-general
    (-> path last (= :collection-form)) :heraldry.component/collection-general
    (-> path last (= :collection)) :heraldry.component/collection
    (-> path drop-last (->> (take-last 2)) (= [:collection :elements])) :heraldry.component/collection-element
    (-> path last (= :render-options)) :heraldry.component/render-options
    (-> path last (= :coat-of-arms)) :heraldry.component/coat-of-arms
    (and (-> path last keyword?)
         (-> path last name (s/starts-with? "cottise"))) :heraldry.component/cottise
    (keyword? raw-type) (type->component-type raw-type)
    :else nil))

(defn state-source [path]
  (if (-> path first (= :context))
    :context
    :state))

(defmulti get-sanitized-data (fn [path _context]
                               (state-source path)))

(defmulti get-raw-data (fn [path _context]
                         (state-source path)))

(defmulti get-list-size (fn [path _context]
                          (state-source path)))

(defn render-option [key {:keys [render-options-path] :as context}]
  (get-sanitized-data (conj render-options-path key) context))

(defmulti render-component (fn [path _parent-path _environment context]
                             (effective-component-type
                              path
                              ;; TODO: need the raw value here for type
                              (get-raw-data (conj path :type) context))))

(defmethod render-component nil [path parent-path _environment _context]
  (log/warn :not-implemented path parent-path)
  [:<>])