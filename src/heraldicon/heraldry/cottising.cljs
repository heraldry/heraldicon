(ns heraldicon.heraldry.cottising
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.line.core :as line]
   [heraldicon.interface :as interface]
   [heraldicon.options :as options]))

(defn add-cottise-options [options key context]
  (let [line-style (-> (line/options (c/++ context :line))
                       (options/override-if-exists [:fimbriation :alignment :default] :outside))
        opposite-line-style (-> (line/options (c/++ context :opposite-line) :inherited-options line-style)
                                (options/override-if-exists [:fimbriation :alignment :default] :outside))]
    (assoc options
           key
           {:line (assoc line-style :ui/label :string.entity/line)
            :opposite-line (assoc opposite-line-style :ui/label :string.entity/opposite-line)
            :distance {:type :option.type/range
                       :min -10
                       :max 20
                       :default 2
                       :ui/label :string.option/distance
                       :ui/step 0.1}
            :thickness {:type :option.type/range
                        :min 0.1
                        :max 20
                        :default 2
                        :ui/label :string.option/thickness
                        :ui/step 0.1}
            :outline? {:type :option.type/boolean
                       :default false
                       :ui/label :string.charge.tincture-modifier.special/outline}
            :ui/element :ui.element/cottising})))

(defn add-cottising [context num]
  (let [cottising-context (c/++ context :cottising)]
    (cond-> {}
      (>= num 1) (->
                   (add-cottise-options :cottise-1 (c/++ cottising-context :cottise-1))
                   (add-cottise-options :cottise-2 (c/++ cottising-context :cottise-2)))
      (>= num 2) (->
                   (add-cottise-options :cottise-opposite-1 (c/++ cottising-context :cottise-opposite-1))
                   (add-cottise-options :cottise-opposite-2 (c/++ cottising-context :cottise-opposite-2)))
      (>= num 3) (->
                   (add-cottise-options :cottise-extra-1 (c/++ cottising-context :cottise-extra-1))
                   (add-cottise-options :cottise-extra-2 (c/++ cottising-context :cottise-extra-2))))))

; :heraldry/cottise is a special case, it's a UI component, but not a :heraldry.options/root,
; because it is a child of the parent :cottising

(defmethod interface/options-subscriptions :heraldry/cottise [_context]
  #{})

(defmethod interface/options :heraldry/cottise [_context]
  nil)

(defn kind [context]
  (-> context :path last))

(defmulti cottise-properties (fn [_context reference-properties]
                               (:type reference-properties)))

(defmethod cottise-properties nil [_context _properties])

(defmethod interface/properties :heraldry/cottise [context]
  (let [reference-context (case (kind context)
                            :cottise-2 (-> context c/-- (c/++ :cottise-1))
                            :cottise-opposite-2 (-> context c/-- (c/++ :cottise-opposite-1))
                            :cottise-extra-2 (-> context c/-- (c/++ :cottise-extra-1))
                            (interface/parent context))
        reference-properties (interface/get-properties reference-context)]
    (cottise-properties context reference-properties)))
