/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package boofcv.alg.feature.orientation.impl;

import boofcv.alg.feature.orientation.GenericOrientationGradientTests;
import boofcv.struct.image.GrayU8;
import org.junit.jupiter.api.Nested;


/**
 * @author Peter Abeles
 */
public class TestImplOrientationAverage_S16 {

	double angleTol = 0.1;

	class Base extends GenericOrientationGradientTests {
		Base( boolean weighted ) {
			super(angleTol,r*2+1, GrayU8.class);
			ImplOrientationAverage_S16 alg = new ImplOrientationAverage_S16(1.0/2.0,weighted);
			alg.setSampleRadius(r);
			setRegionOrientation(alg);
		}
	}

	@Nested
	class Unweighted extends Base {
		Unweighted() {
			super(false);
		}
	}

	@Nested
	class Weighted extends Base {
		Weighted() {
			super(true);
		}
	}
}
