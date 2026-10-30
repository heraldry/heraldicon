(ns heraldicon.interface-test
  (:require
   [cljs.test :refer-macros [are deftest is]]
   [heraldicon.interface :as interface]))

(def ^:private field
  [:arms :coat-of-arms :field])

(deftest parent-contexts
  (are [path node-parent-path parent-path]
       (and (= (:path (interface/parent-node-context {:path path})) node-parent-path)
            (= (:path (interface/parent {:path path})) parent-path))

    ;; the root has no parent
    [:arms :coat-of-arms] nil nil

    ;; a field belongs to the component that owns it
    field [:arms :coat-of-arms] [:arms :coat-of-arms]
    (conj field :components 0 :field) (conj field :components 0) (conj field :components 0)

    ;; list elements belong to the owner of the list
    (conj field :components 0) field field
    (conj field :fields 1) field field
    [:arms :helms :elements 0 :components 1] [:arms :helms :elements 0] [:arms :helms :elements 0]

    ;; a charge in a charge group is a node of the group, but its geometry
    ;; depends on the field the group sits in
    (conj field :components 0 :charges 2) (conj field :components 0) field

    ;; a cottise belongs to the field of its ordinary
    (conj field :components 0 :cottising :cottise-1) field field

    ;; a semé's charge has no parent; its environment is given explicitly
    (conj field :components 0 :charge) nil nil))

(deftest parent-drops-component-data-of-children
  (let [charge-path (conj field :components 0 :charges 2)
        other-path [:arms :helms :elements 0]
        context {:path charge-path
                 :component-data {charge-path {:anchor-override {:x 1 :y 2}}
                                  other-path {:auto-resize? false}}}]
    (is (= (:component-data (interface/parent context))
           {other-path {:auto-resize? false}}))
    (is (= (:component-data (interface/parent-node-context context))
           {other-path {:auto-resize? false}}))))
